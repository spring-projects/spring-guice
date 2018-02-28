/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.guice.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.name.Named;
import com.google.inject.spi.Element;
import com.google.inject.spi.ElementSource;
import com.google.inject.spi.Elements;
import com.google.inject.spi.PrivateElements;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.guice.module.SpringModule;

/**
 * Configuration postprocessor that registers all the bindings in Guice modules as Spring
 * beans.
 * 
 * @author Dave Syer
 * @author Talylor Wicksell
 *
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
class ModuleRegistryConfiguration implements BeanDefinitionRegistryPostProcessor,
		ApplicationContextAware, ApplicationListener<CreateInjectorSignalEvent> {

	private static final String SPRING_GUICE_DEDUPE_BINDINGS_PROPERTY_NAME = "spring.guice.dedup";
	private ApplicationContext applicationContext;
	private List<Module> modules;
	private ConfigurableListableBeanFactory beanFactory;

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
							.equals(entry.getValue().getSource().toString())) {
				continue;
			}

			Binding<?> binding = entry.getValue();
			Key<?> key = entry.getKey();
			Object source = binding.getSource();

			RootBeanDefinition bean = new RootBeanDefinition(GuiceFactoryBean.class);
			ConstructorArgumentValues args = new ConstructorArgumentValues();
			args.addIndexedArgumentValue(0, key.getTypeLiteral().getRawType());
			args.addIndexedArgumentValue(1, key);
			bean.setConstructorArgumentValues(args);
			if (source != null && source instanceof ElementSource) {
				bean.setResourceDescription(
						((ElementSource) source).getDeclaringSource().toString());
			}
			else {
				bean.setResourceDescription(SpringModule.SPRING_GUICE_SOURCE);
			}
			bean.setAttribute(SpringModule.SPRING_GUICE_SOURCE, true);
			registry.registerBeanDefinition(extractName(key), bean);
		}

	}

	private String extractName(Key<?> key) {
		if (key.getAnnotation() instanceof Named) {
			return ((Named) key.getAnnotation()).value();
		}
		return key.getTypeLiteral().getRawType().getSimpleName();
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
			throws BeansException {
		modules = new ArrayList<Module>(((ConfigurableListableBeanFactory) registry)
				.getBeansOfType(Module.class).values());
		modules.add(new SpringModule(this.applicationContext));
		Map<Key<?>, Binding<?>> bindings = new HashMap<Key<?>, Binding<?>>();
		List<Element> elements = Elements.getElements(Stage.TOOL, modules);
		if (applicationContext.getEnvironment().getProperty(
				SPRING_GUICE_DEDUPE_BINDINGS_PROPERTY_NAME, Boolean.class, false)) {
			elements = removeDuplicates(elements);
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

		// This event can be published now and it wont actually be processed until later
		// (during onRefresh()). There's no other way to get a hook into this phase of the
		// lifecycle.
		applicationContext.publishEvent(new CreateInjectorSignalEvent());
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
							&& binding.getSource().equals(compareTo.getSource());
				}
				else {
					return binding.equals(compareTo);
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
		this.beanFactory = beanFactory;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void onApplicationEvent(CreateInjectorSignalEvent event) {
		createInjector(modules, beanFactory);
	}
}

/**
 * Signaling event used to trigger injector creation after BeanPostProcessors have been
 * applied.
 */
class CreateInjectorSignalEvent extends ApplicationEvent {
	private static final long serialVersionUID = -6546970378679850504L;

	public CreateInjectorSignalEvent() {
		super(serialVersionUID);
	}
}
