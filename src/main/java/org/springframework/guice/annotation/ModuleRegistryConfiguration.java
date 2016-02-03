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
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.guice.module.SpringModule;

import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ModuleRegistryConfiguration implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

	ApplicationContext applicationContext;

	private Injector createInjector(Collection<Module> modules) {
		return Guice.createInjector(modules);
	}

	private void mapBindings(Injector injector, BeanDefinitionRegistry registry)
	{
		for (Entry<Key<?>, Binding<?>> entry : injector.getBindings().entrySet()) {
			if (entry.getKey().getTypeLiteral().getRawType().equals(Injector.class) || 
					"spring-guice".equals(entry.getValue().getSource().toString())) {
				continue;
			}
		
			entry.getValue().getKey().toString();
			RootBeanDefinition bean = new RootBeanDefinition(GuiceFactoryBean.class);
			ConstructorArgumentValues args = new ConstructorArgumentValues();
			args.addIndexedArgumentValue(0, entry.getKey().getTypeLiteral().getRawType());
			args.addIndexedArgumentValue(1, entry.getValue().getProvider());
			bean.setConstructorArgumentValues(args);
			registry.registerBeanDefinition(entry.getValue().getKey().toString(), bean);
		}
		
		if(injector.getParent() != null)
		{
			mapBindings(injector.getParent(), registry);
		}
		
		((ConfigurableListableBeanFactory) registry).registerResolvableDependency(Injector.class, injector);
	}


	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		
		
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		List<Module> modules = new ArrayList<Module>(((DefaultListableBeanFactory)registry).getBeansOfType(Module.class).values());
		modules.add(new SpringModule(this.applicationContext));
		Injector injector = createInjector(modules);
		mapBindings(injector, registry);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext=applicationContext;
	}

}