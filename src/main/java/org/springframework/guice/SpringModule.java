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

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.ClassUtils;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provider;

/**
 * @author Dave Syer
 *
 */
public class SpringModule implements Module {

	private DefaultListableBeanFactory beanFactory;
	
	private GuiceModuleMetadata metadata = new GuiceModuleMetadata();

	public SpringModule(GenericApplicationContext context) {
		this.beanFactory = (DefaultListableBeanFactory) context
				.getAutowireCapableBeanFactory();
		if (beanFactory.getBeanNamesForType(GuiceModuleMetadata.class).length>0) {
			this.metadata = beanFactory.getBean(GuiceModuleMetadata.class);
		} else if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, GuiceModuleMetadata.class).length>0) {
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
				Provider<Object> provider = new Provider<Object>() {
					@Override
					public Object get() {
						return beanFactory.getBean(beanName, cls);
					}
				};
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
	}

}
