/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.guice.annotation;

import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.google.inject.name.Named;
import com.google.inject.spi.Element;
import com.google.inject.spi.ElementSource;
import com.google.inject.spi.Elements;
import com.google.inject.spi.PrivateElements;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.Order;
import org.springframework.guice.module.SpringModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Configuration postprocessor that registers all the bindings in Guice modules as Spring
 * beans.
 *
 * @author Dave Syer
 * @author Talylor Wicksell
 * @author Howard Yuan
 *
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
class ModuleRegistryConfiguration
		implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

	private static final String SPRING_GUICE_DEDUPE_BINDINGS_PROPERTY_NAME = "spring.guice.dedup";
	private static final String SPRING_GUICE_AUTOWIRE_JIT_PROPERTY_NAME = "spring.guice.autowireJIT";

	private final Log logger = LogFactory.getLog(getClass());

	private ApplicationContext applicationContext;
	private List<Module> modules;
	private AtomicBoolean injectorCreated = new AtomicBoolean(false);
	private boolean enableJustInTimeBinding = true;

	private void createInjector(List<Module> modules,
			ConfigurableListableBeanFactory beanFactory) {
		Injector injector = null;
		try {
			Map<String, InjectorFactory> beansOfType = beanFactory
					.getBeansOfType(InjectorFactory.class);
			if (beansOfType.size() > 1) {
				throw new ApplicationContextException("Found multiple beans of type "
						+ InjectorFactory.class.getName()
						+ "  Please ensure that only one InjectorFactory bean is defined. InjectorFactory beans found: "
						+ beansOfType.keySet());
			}
			else if (beansOfType.size() == 1) {
				InjectorFactory injectorFactory = beansOfType.values().iterator().next();
				injector = injectorFactory.createInjector(modules);
			}
		}
		catch (NoSuchBeanDefinitionException e) {

		}
		if (injector == null) {
			injector = Guice.createInjector(modules);
		}
		beanFactory.registerResolvableDependency(Injector.class, injector);
		beanFactory.registerSingleton("injector", injector);
	}

	private void mapBindings(Map<Key<?>, Binding<?>> bindings,
			BeanDefinitionRegistry registry) {
		for (Entry<Key<?>, Binding<?>> entry : bindings.entrySet()) {
			if (entry.getKey().getTypeLiteral().getRawType().equals(Injector.class)
					|| SpringModule.SPRING_GUICE_SOURCE
							.equals(Optional.ofNullable(entry.getValue().getSource()).map(Object::toString).orElse(""))) {
				continue;
			}
			if (entry.getKey().getAnnotationType() != null &&
					entry.getKey().getAnnotationType().getName()
							.startsWith("com.google.inject.multibindings")) {
				continue;
			}

			Binding<?> binding = entry.getValue();
			Key<?> key = entry.getKey();
			Object source = binding.getSource();

			RootBeanDefinition bean = new RootBeanDefinition(GuiceFactoryBean.class);
			ConstructorArgumentValues args = new ConstructorArgumentValues();
			args.addIndexedArgumentValue(0, key.getTypeLiteral().getRawType());
			args.addIndexedArgumentValue(1, key);
			args.addIndexedArgumentValue(2, Scopes.isSingleton(binding));
			bean.setConstructorArgumentValues(args);
			bean.setTargetType(ResolvableType.forType(key.getTypeLiteral().getType()));
			if(!Scopes.isSingleton(binding)) {
				bean.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);
			}
			if (source instanceof ElementSource) {
				bean.setResourceDescription(
						((ElementSource) source).getDeclaringSource().toString());
			}
			else {
				bean.setResourceDescription(SpringModule.SPRING_GUICE_SOURCE);
			}
			bean.setAttribute(SpringModule.SPRING_GUICE_SOURCE, true);
			if (key.getAnnotationType() != null) {
				bean.addQualifier(new AutowireCandidateQualifier(Qualifier.class,
						getValueAttributeForNamed(key)));
				bean.addQualifier(new AutowireCandidateQualifier(key.getAnnotationType(), getValueAttributeForNamed(key)));
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
		else if (key.getAnnotation() instanceof javax.inject.Named) {
			return ((javax.inject.Named) key.getAnnotation()).value();
		}
		else if (key.getAnnotationType() != null){
			return key.getAnnotationType().getName();
		}
		else {
			return null;
		}
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
			throws BeansException {
		modules = new ArrayList<Module>(((ConfigurableListableBeanFactory) registry)
				.getBeansOfType(Module.class).values());
		modules.add(new SpringModule((ConfigurableListableBeanFactory) registry, enableJustInTimeBinding));
		Map<Key<?>, Binding<?>> bindings = new HashMap<Key<?>, Binding<?>>();
		List<Element> elements = Elements.getElements(Stage.TOOL, modules);
		if (applicationContext.getEnvironment().getProperty(
				SPRING_GUICE_DEDUPE_BINDINGS_PROPERTY_NAME, Boolean.class, false)) {
			elements = removeDuplicates(elements);
			modules = Collections.singletonList(Elements.getModule(elements));
		}
		if (applicationContext.getEnvironment().containsProperty("spring.guice.modules.exclude")) {
			String[] modulesToFilter = applicationContext.getEnvironment()
					.getProperty("spring.guice.modules.exclude", "").split(",");
			elements = elements.stream().filter(e -> elementFilter(modulesToFilter, e)).collect(Collectors.toList());
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
		mapBindings(bindings, registry);
	}

	private boolean elementFilter(String[] modulesToFilter, Element element){
		try {
			return Arrays.stream(modulesToFilter)
					.noneMatch(ex -> Optional.of(element).map(Element::getSource).map(Object::toString).orElse("").contains(ex));
		} catch (Exception e){
			logger.error(String.format("Unable fo filter element[%s] with filter [%s]", element, Arrays.toString(modulesToFilter)), e);
			return false;
		}
	}

	private void extractPrivateElements(Map<Key<?>, Binding<?>> bindings,
			PrivateElements privateElements) {
		List<Element> elements = privateElements.getElements();
		for (Element e : elements) {
			if (e instanceof Binding && privateElements.getExposedKeys()
					.contains(((Binding<?>) e).getKey())) {
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
	 * for a given binding key
	 */
	protected List<Element> removeDuplicates(List<Element> elements) {
		List<Element> duplicateElements = elements.stream()
				.filter(e -> e instanceof Binding).map(e -> (Binding<?>) e)
				.collect(Collectors.groupingBy(Binding::getKey)).entrySet().stream()
				.filter(e -> e.getValue().size() > 1 && e.getValue().stream().anyMatch(
						binding -> binding.getSource() != null && binding.getSource()
								.toString().contains(SpringModule.SPRING_GUICE_SOURCE))) // find
																							// duplicates
				.flatMap(e -> e.getValue().stream())
				.filter(e -> e.getSource() != null && !e.getSource().toString()
						.contains(SpringModule.SPRING_GUICE_SOURCE))
				.collect(Collectors.toList());

		@SuppressWarnings("unlikely-arg-type")
		List<Element> dedupedElements = elements.stream().filter(e -> {
			if (e instanceof Binding) {
				return !duplicateElements
						.contains(new SourceComparableBinding((Binding<?>) e));
			}
			else {
				return true;
			}
		}).collect(Collectors.toList());
		return dedupedElements;
	}

	private static class SourceComparableBinding {
		private Binding<?> binding;

		public SourceComparableBinding(Binding<?> binding) {
			this.binding = binding;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Binding) {
				Binding<?> compareTo = (Binding<?>) obj;
				if (compareTo.getSource() != null && this.binding != null) {
					return binding.equals(compareTo)
							&& Objects.equals(binding.getSource(), compareTo.getSource());
				}
				else {
					return Objects.equals(binding, compareTo);
				}
			}
			else {
				return false;
			}
		}
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		beanFactory.registerSingleton("guiceInjectorInitializer",
				new GuiceInjectorInitializingBeanPostProcessor() {
					@Override
					public Object postProcessBeforeInitialization(Object bean,
							String beanName) throws BeansException {
						return bean;
					}

					@Override
					public Object postProcessAfterInitialization(Object bean,
							String beanName) throws BeansException {
						if (injectorCreated.compareAndSet(false, true)) {
							createInjector(modules, beanFactory);
						}
						return bean;
					}
				});
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
		this.enableJustInTimeBinding = applicationContext.getEnvironment()
				.getProperty(SPRING_GUICE_AUTOWIRE_JIT_PROPERTY_NAME, Boolean.class, true);
	}

	private static class GuiceInjectorInitializingBeanPostProcessor
			implements BeanPostProcessor, Ordered {
		@Override
		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE - 1;
		}
	}
}
