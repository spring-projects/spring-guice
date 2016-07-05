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

package org.springframework.guice.module;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.name.Names;

/**
 * @author Dave Syer
 *
 */
public class SpringModule implements Module {

	private DefaultListableBeanFactory beanFactory;

	private BindingTypeMatcher matcher = new GuiceModuleMetadata();

	private Map<Class<?>, Provider<?>> bound = new HashMap<Class<?>, Provider<?>>();

	public SpringModule(ApplicationContext context) {
		this((DefaultListableBeanFactory) context.getAutowireCapableBeanFactory());
	}

	public SpringModule(DefaultListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		if (beanFactory.getBeanNamesForType(GuiceModuleMetadata.class).length > 0) {
			this.matcher = new CompositeTypeMatcher(beanFactory.getBeansOfType(GuiceModuleMetadata.class).values());
		}
	}

	@Override
	public void configure(Binder binder) {
		for (String name : this.beanFactory.getBeanDefinitionNames()) {
			BeanDefinition definition = this.beanFactory.getBeanDefinition(name);
			if (definition.isAutowireCandidate() && definition.getRole() == AbstractBeanDefinition.ROLE_APPLICATION) {
				Class<?> type = this.beanFactory.getType(name);
				@SuppressWarnings("unchecked")
				final Class<Object> cls = (Class<Object>) type;
				final String beanName = name;
				Provider<Object> provider = new BeanFactoryProvider(this.beanFactory, beanName, type);
				if (!cls.isInterface() && !ClassUtils.isCglibProxyClass(cls)) {
					bindConditionally(binder, name, cls, provider);
				}
				for (Class<?> iface : ClassUtils.getAllInterfacesForClass(cls)) {
					@SuppressWarnings("unchecked")
					Class<Object> unchecked = (Class<Object>) iface;
					bindConditionally(binder, name, unchecked, provider);
				}
			}
		}
	}

	private void bindConditionally(Binder binder, String name, Class<Object> type, Provider<Object> provider) {
		if (this.bound.get(type) != null) {
			// Only bind one provider for each type
			return; // TODO: named beans
		}
		if (!this.matcher.matches(name, type)) {
			return;
		}
		if (type.getName().startsWith("com.google.inject")) {
			return;
		}
		binder.withSource("spring-guice").bind(type).toProvider(provider);
		binder.withSource("spring-guice").bind(type).annotatedWith(Names.named(name)).toProvider(provider);
		this.bound.put(type, provider);
	}

	private static class BeanFactoryProvider implements Provider<Object> {

		private DefaultListableBeanFactory beanFactory;

		private String name;

		private Class<?> type;

		private Object result;

		public BeanFactoryProvider(DefaultListableBeanFactory beanFactory, String name, Class<?> type) {
			this.beanFactory = beanFactory;
			this.name = name;
			this.type = type;
		}

		@Override
		public Object get() {
			if (this.result == null) {

				String[] named = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory, this.type);
				List<String> names = new ArrayList<>(named.length);
				for (String name : named) {
					if (name.equals(this.name)) names.add(name);
				}

				if (names.size() == 1) {
					this.result = this.beanFactory.getBean(this.name, this.type);
				}
				else {
					for (String name : names) {
						if (this.beanFactory.getBeanDefinition(name).isPrimary()) {
							this.result = this.beanFactory.getBean(name, this.type);
							break;
						}
					}
					if (this.result == null) {
						throw new ProvisionException("No primary bean definition for type: " + this.type);
					}
				}
			}
			return this.result;
		}
	}

	private static class CompositeTypeMatcher implements BindingTypeMatcher {
		private Collection<? extends BindingTypeMatcher> matchers;

		public CompositeTypeMatcher(Collection<? extends BindingTypeMatcher> matchers) {
			this.matchers = matchers;
		}

		@Override
		public boolean matches(String name, Class<?> type) {
			for (BindingTypeMatcher matcher : this.matchers) {
				if (matcher.matches(name, type)) {
					return true;
				}
			}
			return false;
		}
	}

}
