/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.guice.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Binding;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.internal.BindingImpl;
import com.google.inject.name.Named;
import com.google.inject.spi.Element;
import com.google.inject.spi.ElementSource;
import com.google.inject.spi.Elements;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.Message;
import com.google.inject.spi.PrivateElements;
import com.google.inject.spi.UntargettedBinding;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.Order;
import org.springframework.guice.module.SpringModule;

/**
 * Configuration postprocessor that registers all the bindings in Guice modules as Spring
 * beans.
 *
 * @author Dave Syer
 * @author Talylor Wicksell
 * @author Howard Yuan
 *
 */
@Configuration(proxyBeanMethods = false)
@Order(Ordered.HIGHEST_PRECEDENCE)
class ModuleRegistryConfiguration implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

	private static final String SPRING_GUICE_DEDUPE_BINDINGS_PROPERTY_NAME = "spring.guice.dedup";

	private static final String SPRING_GUICE_AUTOWIRE_JIT_PROPERTY_NAME = "spring.guice.autowireJIT";

	private static final String SPRING_GUICE_STAGE_PROPERTY_NAME = "spring.guice.stage";

	private static final List<String> SPRING_GUICE_IGNORED_ANNOTATION_PREFIXES = Arrays.asList(
			"com.google.inject.multibindings", "com.google.inject.internal.Element",
			"com.google.inject.internal.UniqueAnnotations", "com.google.inject.internal.RealOptionalBinder");

	private final Log logger = LogFactory.getLog(getClass());

	private ApplicationContext applicationContext;

	private boolean enableJustInTimeBinding = true;

	protected static final Method GUICE_BINDINGIMPL_WITHKEY;

	static {
		try {
			GUICE_BINDINGIMPL_WITHKEY = BindingImpl.class.getDeclaredMethod("withKey", Key.class);
			GUICE_BINDINGIMPL_WITHKEY.setAccessible(true);
		}
		catch (NoSuchMethodException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		this.enableJustInTimeBinding = applicationContext.getEnvironment()
				.getProperty(SPRING_GUICE_AUTOWIRE_JIT_PROPERTY_NAME, Boolean.class, true);
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		List<Module> modules = filterModules(registry,
				new ArrayList<>(((ConfigurableListableBeanFactory) registry).getBeansOfType(Module.class).values()));
		SpringModule module = new SpringModule((ConfigurableListableBeanFactory) registry,
				this.enableJustInTimeBinding);
		modules.add(module);
		Map<Key<?>, Binding<?>> bindings = new HashMap<Key<?>, Binding<?>>();
		List<Element> elements = Elements.getElements(Stage.TOOL, modules);
		List<Message> errors = elements.stream().filter((e) -> e instanceof Message).map((e) -> (Message) e)
				.collect(Collectors.toList());
		if (!errors.isEmpty()) {
			throw new ConfigurationException(errors);
		}
		if (this.applicationContext.getEnvironment().getProperty(SPRING_GUICE_DEDUPE_BINDINGS_PROPERTY_NAME,
				Boolean.class, false)) {
			elements = removeDuplicates(elements);
			modules = Collections.singletonList(Elements.getModule(elements));
		}
		if (this.applicationContext.getEnvironment().containsProperty("spring.guice.modules.exclude")) {
			String[] modulesToFilter = this.applicationContext.getEnvironment()
					.getProperty("spring.guice.modules.exclude", "").split(",");
			elements = elements.stream().filter((e) -> elementFilter(modulesToFilter, e)).collect(Collectors.toList());
			modules = Collections.singletonList(Elements.getModule(elements));
		}
		for (Element e : elements) {
			if (e instanceof Binding) {
				Binding<?> binding = (Binding<?>) e;
				bindings.put(binding.getKey(), binding);
			}
			else if (e instanceof PrivateElements) {
				extractPrivateElements(bindings, (PrivateElements) e);
			}
		}
		mapBindings(bindings, registry, module);

		// Register the injector initializer
		RootBeanDefinition beanDefinition = new RootBeanDefinition(GuiceInjectorInitializer.class);
		final List<Module> finalModules = new ArrayList<>(modules);
		beanDefinition.setInstanceSupplier(() -> new GuiceInjectorInitializer(finalModules,
				(ConfigurableApplicationContext) this.applicationContext));
		beanDefinition.setAttribute(SpringModule.SPRING_GUICE_SOURCE, true);
		registry.registerBeanDefinition("guiceInjectorInitializer", beanDefinition);
	}

	private List<Module> filterModules(BeanDefinitionRegistry registry, List<Module> modules) {
		Map<String, ModuleFilter> moduleFilters = ((ConfigurableListableBeanFactory) registry)
				.getBeansOfType(ModuleFilter.class);
		Predicate<Module> moduleFilter = (m) -> true;
		for (Predicate<Module> value : moduleFilters.values()) {
			moduleFilter = moduleFilter.and(value);
		}
		return modules.stream().filter(moduleFilter).collect(Collectors.toList());
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) {

	}

	private void mapBindings(Map<Key<?>, Binding<?>> bindings, BeanDefinitionRegistry registry, SpringModule module) {
		Stage stage = this.applicationContext.getEnvironment().getProperty(SPRING_GUICE_STAGE_PROPERTY_NAME,
				Stage.class, Stage.PRODUCTION);
		boolean ifLazyInit = stage.equals(Stage.DEVELOPMENT);
		Map<? extends Key<?>, List<LinkedKeyBinding<?>>> linkedBindingsByKey = bindings.values().stream()
				.filter((e) -> e instanceof LinkedKeyBinding).map((e) -> ((LinkedKeyBinding<?>) e))
				.collect(Collectors.groupingBy(LinkedKeyBinding::getLinkedKey));

		Map<? extends Key<?>, ? extends Binding<?>> guiceBindingsByKey = bindings.entrySet().stream()
				.filter((entry) -> {
					Binding<?> binding = entry.getValue();
					Key<?> key = entry.getKey();
					Object source = binding.getSource();

					TypeLiteral<?> typeLiteral = key.getTypeLiteral();
					Class<? extends Annotation> annotationType = key.getAnnotationType();

					if (binding instanceof UntargettedBinding && linkedBindingsByKey.containsKey(binding.getKey())) {
						return false;
					}
					if (typeLiteral.getRawType().equals(Injector.class) || SpringModule.SPRING_GUICE_SOURCE
							.equals(Optional.ofNullable(source).map(Object::toString).orElse(""))) {
						return false;
					}
					if (annotationType != null) {
						if (SPRING_GUICE_IGNORED_ANNOTATION_PREFIXES.stream()
								.anyMatch((prefix) -> annotationType.getName().startsWith(prefix))) {
							return false;
						}
					}
					return true;
				}).collect(Collectors.toMap(Entry::getKey, Entry::getValue));

		for (Entry<? extends Key<?>, ? extends Binding<?>> entry : guiceBindingsByKey.entrySet()) {
			Binding<?> binding = entry.getValue();
			Key<?> key = entry.getKey();

			TypeLiteral<?> typeLiteral = key.getTypeLiteral();
			Class<? extends Annotation> annotationType = key.getAnnotationType();

			RootBeanDefinition bean = new RootBeanDefinition(GuiceFactoryBean.class);
			bean.setInstanceSupplier(() -> {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				GuiceFactoryBean factory = new GuiceFactoryBean(typeLiteral.getRawType(), key,
						Scopes.isSingleton(binding), module.getInjector());
				return factory;
			});
			bean.setTargetType(ResolvableType.forType(typeLiteral.getType()));
			if (!Scopes.isSingleton(binding)) {
				bean.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);
			}
			Object source = binding.getSource();
			if (source instanceof ElementSource) {
				bean.setResourceDescription(((ElementSource) source).getDeclaringSource().toString());
			}
			else {
				bean.setResourceDescription(SpringModule.SPRING_GUICE_SOURCE);
			}
			bean.setAttribute(SpringModule.SPRING_GUICE_SOURCE, true);
			if (annotationType != null) {
				String nameValue = getValueAttributeForNamed(key);
				bean.addQualifier(new AutowireCandidateQualifier(Qualifier.class, nameValue));
				bean.addQualifier(new AutowireCandidateQualifier(annotationType, nameValue));
			}
			if (ifLazyInit) {
				bean.setLazyInit(true);
			}
			registry.registerBeanDefinition(extractName(key), bean);
		}

	}

	private String extractName(Key<?> key) {
		final String className = key.getTypeLiteral().getType().getTypeName();
		String valueAttribute = getValueAttributeForNamed(key);
		if (valueAttribute != null) {
			return valueAttribute + "_" + className;
		}
		else {
			return className;
		}
	}

	private String getValueAttributeForNamed(Key<?> key) {
		if (key.getAnnotation() instanceof Named) {
			return ((Named) key.getAnnotation()).value();
		}
		else if (key.getAnnotation() instanceof jakarta.inject.Named) {
			return ((jakarta.inject.Named) key.getAnnotation()).value();
		}
		else if (key.getAnnotationType() != null) {
			String value = key.getAnnotationType().getName();

			if (key.getAnnotation() != null) {
				// Edge case when the Named annotation is wrapped
				String annotationString = key.getAnnotation().toString();
				String wrappedNamedValue = substringBetween(annotationString, "@com.google.inject.name.Named(\"",
						"\")");
				if (wrappedNamedValue != null) {
					value = wrappedNamedValue + "_" + value;
				}
			}

			return value;
		}
		else {
			return null;
		}
	}

	private boolean elementFilter(String[] modulesToFilter, Element element) {
		try {
			return Arrays.stream(modulesToFilter).noneMatch(
					(ex) -> Optional.of(element).map(Element::getSource).map(Object::toString).orElse("").contains(ex));
		}
		catch (Exception ex) {
			this.logger.error(String.format("Unable fo filter element[%s] with filter [%s]", element,
					Arrays.toString(modulesToFilter)), ex);
			return false;
		}
	}

	private void extractPrivateElements(Map<Key<?>, Binding<?>> bindings, PrivateElements privateElements) {
		List<Element> elements = privateElements.getElements();
		for (Element e : elements) {
			if (e instanceof Binding && privateElements.getExposedKeys().contains(((Binding<?>) e).getKey())) {
				Binding<?> binding = (Binding<?>) e;
				bindings.put(binding.getKey(), binding);
			}
			else if (e instanceof PrivateElements) {
				extractPrivateElements(bindings, (PrivateElements) e);
			}
		}
	}

	/***
	 * Remove guice-sourced bindings in favor of spring-sourced bindings, when both exist
	 * for a given binding key.
	 * @param elements list of elements to de-duplicate
	 * @return de-duplicated list of bindings
	 */
	protected List<Element> removeDuplicates(List<Element> elements) {
		Predicate<Element> hasSpringSource = ((element) -> element.getSource() != null
				&& element.getSource().toString().contains(SpringModule.SPRING_GUICE_SOURCE));

		List<? extends Binding<?>> bindings = elements.stream().filter((e) -> e instanceof Binding)
				.map((e) -> (Binding<?>) e).collect(Collectors.toList());

		Map<? extends Key<?>, ? extends Key<?>> injectionKeys = bindings.stream()
				.collect(Collectors.groupingBy(Binding::getKey)).entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, (e) -> {
					List<? extends Binding<?>> keyBindings = e.getValue();
					if (keyBindings.size() == 1) {
						// If a linked binding isn't duplicated by its key, try the linked
						// injection key
						Binding<?> binding = keyBindings.get(0);
						if (binding instanceof LinkedKeyBinding) {
							return ((LinkedKeyBinding<?>) binding).getLinkedKey();
						}
					}
					return e.getKey();
				}));

		Map<? extends Key<?>, List<? extends Binding<?>>> duplicateBindings = bindings.stream()
				.collect(Collectors.groupingBy((e) -> injectionKeys.get(e.getKey()))).entrySet().stream()
				.filter((e) -> e.getValue().size() > 1 && e.getValue().stream().anyMatch(hasSpringSource))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));

		return elements.stream().flatMap((e) -> {
			if (e instanceof Binding) {
				Binding<?> b = (Binding<?>) e;
				Key<?> key = injectionKeys.get(b.getKey());
				List<? extends Binding<?>> duplicates = duplicateBindings.get(key);
				if (duplicates != null) {
					if (hasSpringSource.test(b)) {
						return duplicates.stream().filter(hasSpringSource.negate())
								.map((guiceBinding) -> withKey(b, guiceBinding.getKey()));
					}
					else {
						// Remove the duplicate Guice binding
						return Stream.empty();
					}
				}
			}
			return Stream.of(e);
		}).collect(Collectors.toList());
	}

	/*
	 * Re-key the Spring source binding with the Guice key for the built-in bindings.
	 */
	private Binding<?> withKey(Binding<?> binding, Key<?> key) {
		try {
			return (BindingImpl<?>) GUICE_BINDINGIMPL_WITHKEY.invoke(binding, key);
		}
		catch (IllegalAccessException | InvocationTargetException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static String substringBetween(String str, String open, String close) {
		if (str == null || open == null || close == null) {
			return null;
		}
		int start = str.indexOf(open);
		if (start != -1) {
			int end = str.indexOf(close, start + open.length());
			if (end != -1) {
				return str.substring(start + open.length(), end);
			}
		}
		return null;
	}

	/**
	 * Creates the Guice injector and registers it.
	 *
	 * The correct time to create the injector is after all Bean Post Processors were
	 * registered (after the registerBeanPostProcessors() phase), but before other beans
	 * get resolved. To achieve this, we create the injector when the first bean gets
	 * resolved - in its post-processing phase. However, this creates a possibility for a
	 * circular initialization error (i.e. if the first bean is also being dependant on by
	 * a Guice provided binding). To resolve this we publish an event that will be
	 * triggered in the registerListeners() phase, and create the injector then. Combining
	 * both initialization mechanisms (post-processor and the event publishing) ensures
	 * the injector will be created no later then the registerListeners() phase, but after
	 * the registerBeanPostProcessors() phase. For application contexts that override
	 * onRefresh() and create beans then (i.e. WebServer based application contexts) the
	 * post-processor initialization will kick-in and create the injector before.
	 */
	static class GuiceInjectorInitializer
			implements BeanPostProcessor, ApplicationListener<GuiceInjectorInitializer.CreateInjectorEvent> {

		private final AtomicBoolean injectorCreated = new AtomicBoolean(false);

		private final List<Module> modules;

		private final ConfigurableApplicationContext applicationContext;

		GuiceInjectorInitializer(List<Module> modules, ConfigurableApplicationContext applicationContext) {
			this.modules = modules;
			this.applicationContext = applicationContext;

			applicationContext.publishEvent(new CreateInjectorEvent());
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (this.injectorCreated.compareAndSet(false, true)) {
				createInjector();
			}
			return bean;
		}

		@Override
		public void onApplicationEvent(CreateInjectorEvent event) {
			if (this.injectorCreated.compareAndSet(false, true)) {
				createInjector();
			}
		}

		private void createInjector() {
			Injector injector = null;
			try {
				Map<String, InjectorFactory> beansOfType = this.applicationContext
						.getBeansOfType(InjectorFactory.class);
				if (beansOfType.size() > 1) {
					throw new ApplicationContextException("Found multiple beans of type "
							+ InjectorFactory.class.getName()
							+ "  Please ensure that only one InjectorFactory bean is defined. InjectorFactory beans found: "
							+ beansOfType.keySet());
				}
				else if (beansOfType.size() == 1) {
					InjectorFactory injectorFactory = beansOfType.values().iterator().next();
					injector = injectorFactory.createInjector(this.modules);
				}
			}
			catch (NoSuchBeanDefinitionException ex) {

			}
			if (injector == null) {
				injector = Guice.createInjector(this.modules);
			}
			this.applicationContext.getBeanFactory().registerResolvableDependency(Injector.class, injector);
			this.applicationContext.getBeanFactory().registerSingleton("injector", injector);
		}

		static class CreateInjectorEvent extends ApplicationEvent {

			private static final long serialVersionUID = -6546970378679850504L;

			CreateInjectorEvent() {
				super(serialVersionUID);
			}

		}

	}

}
