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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

/**
 * @author Dave Syer
 *
 */
public class SpringModule implements Module {

	private DefaultListableBeanFactory beanFactory;

	private BindingTypeMatcher matcher = new GuiceModuleMetadata();

	private Map<Type, Provider<?>> bound = new HashMap<Type, Provider<?>>();

	public SpringModule(ApplicationContext context) {
		this((DefaultListableBeanFactory) context.getAutowireCapableBeanFactory());
	}

	public SpringModule(DefaultListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		if (beanFactory.getBeanNamesForType(GuiceModuleMetadata.class).length > 0) {
			this.matcher = new CompositeTypeMatcher(beanFactory.getBeansOfType(GuiceModuleMetadata.class).values());
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void configure(Binder binder) {
		for (String name : this.beanFactory.getBeanDefinitionNames()) {
			BeanDefinition definition = this.beanFactory.getBeanDefinition(name);
			if (definition.isAutowireCandidate() && definition.getRole() == AbstractBeanDefinition.ROLE_APPLICATION) {
				Class<?> type = this.beanFactory.getType(name);
				final String beanName = name;
				Provider<Object> typeProvider = new BeanFactoryProvider(this.beanFactory, null, type);
				Provider<Object> namedProvider = new BeanFactoryProvider(this.beanFactory, beanName, type);
				if (!type.isInterface() && !ClassUtils.isCglibProxyClass(type)) {
					bindConditionally(binder, name, type, typeProvider, namedProvider);
				}
				for (Class<?> iface : ClassUtils.getAllInterfacesForClass(type)) {
					bindConditionally(binder, name, iface, typeProvider, namedProvider);
				}
				for (Type iface : type.getGenericInterfaces()) {
					bindConditionally(binder, name, iface, typeProvider, namedProvider);
				}
			}
		}
	}

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T> void bindConditionally(Binder binder, String name, Type type, Provider typeProvider,
			Provider namedProvider) {
		if (!this.matcher.matches(name, type)) {
			return;
		}
		if (type.getTypeName().startsWith("com.google.inject")) {
			return;
		}

		if (this.bound.get(type) == null) {
			// Only bind one provider for each type
	        binder.withSource("spring-guice").bind(Key.get(type)).toProvider(typeProvider);
	        this.bound.put(type, typeProvider);
		}
		// But allow binding to named beans
		binder.withSource("spring-guice").bind(TypeLiteral.get(type)).annotatedWith(Names.named(name)).toProvider(namedProvider);
	}

	private static class BeanFactoryProvider<T> implements Provider<T> {

		private DefaultListableBeanFactory beanFactory;

		private String name;

		private Class<T> type;

		private T result;

		public BeanFactoryProvider(DefaultListableBeanFactory beanFactory, String name, Class<T> type) {
			this.beanFactory = beanFactory;
			this.name = name;
			this.type = type;
		}

		@Override
		public T get() {
			if (this.result == null) {

				String[] named = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory, this.type);
				List<String> names = new ArrayList<String>(named.length);
				if (named.length == 1) {
					names.add(named[0]);
				} else {
					for (String name : named) {
						if (name.equals(this.name))
							names.add(name);
					}
				}
				if (names.size() == 1) {
					this.result = this.beanFactory.getBean(names.get(0), this.type);
				} else {
					for (String name : named) {
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
		public boolean matches(String name, Type type) {
			for (BindingTypeMatcher matcher : this.matchers) {
				if (matcher.matches(name, type)) {
					return true;
				}
			}
			return false;
		}
	}

}
