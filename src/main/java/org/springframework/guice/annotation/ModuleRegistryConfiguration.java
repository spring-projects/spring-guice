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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

	ApplicationContext applicationContext;
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
	}

	private void mapBindings(Map<Key<?>, Binding<?>> bindings,
			BeanDefinitionRegistry registry) {
		for (Entry<Key<?>, Binding<?>> entry : bindings.entrySet()) {
			if (entry.getKey().getTypeLiteral().getRawType().equals(Injector.class)
					|| "spring-guice".equals(entry.getValue().getSource().toString())) {
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
				bean.setResourceDescription("spring-guice");
			}
			bean.setAttribute("spring-guice", true);
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

		Map<Key<?>, Binding<?>> bindings = new HashMap<Key<?>, Binding<?>>();
		for (Element e : Elements.getElements(Stage.TOOL, modules)) {
			if (e instanceof Binding) {
				Binding<?> binding = (Binding<?>) e;
				bindings.put(binding.getKey(), binding);
			}
		}
		mapBindings(bindings, registry);
		modules.add(new SpringModule(this.applicationContext));
		// This event can be published now and it wont actually be processed until later
		// (during onRefresh()). There's no other way to get a hook into this phase of the
		// lifecycle.
		applicationContext.publishEvent(new CreateInjectorSignalEvent());
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
