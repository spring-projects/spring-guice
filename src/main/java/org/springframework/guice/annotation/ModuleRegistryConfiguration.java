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
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.guice.module.SpringModule;

import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ModuleRegistryConfiguration implements BeanPostProcessor {

	@Autowired
	private DefaultListableBeanFactory beanFactory;
	
	@Autowired(required=false)
	private List<Module> modules = Collections.emptyList();

	private Injector injector;
	
	@Bean
	public Injector injector() {
		return injector;
	}

	@PostConstruct
	public void init() {
		List<Module> modules = new ArrayList<Module>(this.modules);
		modules.add(new SpringModule(beanFactory));
		injector = Guice.createInjector(modules);
		for (Entry<Key<?>, Binding<?>> entry : injector.getBindings().entrySet()) {
			if (entry.getKey().getTypeLiteral().getRawType().equals(Injector.class)) {
				continue;
			}
			final Provider<?> provider = entry.getValue().getProvider();
			beanFactory.registerResolvableDependency(entry.getKey().getTypeLiteral().getRawType(), new ObjectFactory<Object>() {
				@Override
				public Object getObject() throws BeansException {
					return provider.get();
				}
			});
		}
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}