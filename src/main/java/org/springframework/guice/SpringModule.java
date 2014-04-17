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

package org.springframework.guice;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.ClassUtils;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;

/**
 * @author Dave Syer
 *
 */
public class SpringModule implements Module {

	private DefaultListableBeanFactory beanFactory;

	private GuiceModuleMetadata metadata = new GuiceModuleMetadata();

	private Map<Class<?>, Provider<?>> bound = new HashMap<Class<?>, Provider<?>>();

	public SpringModule(GenericApplicationContext context) {
		this.beanFactory = (DefaultListableBeanFactory) context
				.getAutowireCapableBeanFactory();
		if (beanFactory.getBeanNamesForType(GuiceModuleMetadata.class).length > 0) {
			this.metadata = beanFactory.getBean(GuiceModuleMetadata.class);
		} else if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory,
				GuiceModuleMetadata.class).length > 0) {
			this.metadata = beanFactory.getBean(GuiceModuleMetadata.class);
		}
	}

	@Override
	public void configure(Binder binder) {
		for (String name : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition definition = beanFactory.getBeanDefinition(name);
			if (definition.isAutowireCandidate()
					&& definition.getRole() == AbstractBeanDefinition.ROLE_APPLICATION) {
				Class<?> type = beanFactory.getType(name);
				@SuppressWarnings("unchecked")
				final Class<Object> cls = (Class<Object>) type;
				final String beanName = name;
				Provider<Object> provider = new BeanFactoryProvider(beanFactory,
						beanName, type);
				if (bound.get(type) != null) {
					continue; // TODO: named beans
				}
				if (!cls.isInterface() && !ClassUtils.isCglibProxyClass(cls)) {
					bindConditionally(binder, cls, provider);
				}
				for (Class<?> iface : ClassUtils.getAllInterfacesForClass(cls)) {
					@SuppressWarnings("unchecked")
					Class<Object> unchecked = (Class<Object>) iface;
					bindConditionally(binder, unchecked, provider);
				}
			}
		}
	}

	private void bindConditionally(Binder binder, Class<Object> cls,
			Provider<Object> provider) {
		if (!metadata.matches(cls)) {
			return;
		}
		binder.bind(cls).toProvider(provider);
		bound.put(cls, provider);
	}

	private static class BeanFactoryProvider implements Provider<Object> {

		private DefaultListableBeanFactory beanFactory;
		private String name;
		private Class<?> type;
		private Object result;

		public BeanFactoryProvider(DefaultListableBeanFactory beanFactory, String name,
				Class<?> type) {
			this.beanFactory = beanFactory;
			this.name = name;
			this.type = type;
		}

		@Override
		public Object get() {
			if (result == null) {
				String[] names = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
						beanFactory, type);
				if (names.length == 1) {
					result = beanFactory.getBean(name, type);
				} else {
					for (String name : names) {
						if (beanFactory.getBeanDefinition(name).isPrimary()) {
							result = beanFactory.getBean(name, type);
							break;
						}
					}
					if (result == null) {
						throw new ProvisionException(
								"No primary bean definition for type: " + type);
					}
				}
			}
			return result;
		}
	}

}
