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

package org.springframework.guice.injector;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.spi.TypeConverterBinding;

public class SpringInjector implements Injector {

	private Injector injector;
	private DefaultListableBeanFactory beanFactory;

	public SpringInjector(ApplicationContext context) {
		this.beanFactory = (DefaultListableBeanFactory) context.getAutowireCapableBeanFactory();
		if (context.getBeanNamesForType(Injector.class, true, false).length>0) {
			this.injector = context.getBean(Injector.class);
		}
	}

	@Override
	public void injectMembers(Object instance) {
		this.beanFactory.autowireBean(instance);
	}

	@Override
	public <T> MembersInjector<T> getMembersInjector(TypeLiteral<T> typeLiteral) {
		return new MembersInjector<T>() {
			@Override
			public void injectMembers(T instance) {
				SpringInjector.this.beanFactory.autowireBean(instance);
			}
		};
	}

	@Override
	public <T> MembersInjector<T> getMembersInjector(Class<T> type) {
		return getMembersInjector(TypeLiteral.get(type));
	}

	@Override
	public Map<Key<?>, Binding<?>> getBindings() {
		return null;
	}

	@Override
	public Map<Key<?>, Binding<?>> getAllBindings() {
		return null;
	}

	@Override
	public <T> Binding<T> getBinding(Key<T> key) {
		return null;
	}

	@Override
	public <T> Binding<T> getBinding(Class<T> type) {
		return null;
	}

	@Override
	public <T> Binding<T> getExistingBinding(Key<T> key) {
		return null;
	}

	@Override
	public <T> List<Binding<T>> findBindingsByType(TypeLiteral<T> type) {
		return null;
	}

	@Override
	public <T> Provider<T> getProvider(Key<T> key) {
		// TODO: support for other metadata in the key
		Class<? super T> type = key.getTypeLiteral().getRawType();
		final String name = extractName(key);
		if (this.beanFactory.getBeanNamesForType(type, true, false).length==0) {
			if (this.injector!=null) {
				return this.injector.getProvider(key);
			}
			// TODO: use prototype scope?
			this.beanFactory.registerBeanDefinition(name, new RootBeanDefinition(type));
		}
		if (this.beanFactory.containsBean(name) && this.beanFactory.isTypeMatch(name, type)) {
			return new Provider<T>() {
				@SuppressWarnings("unchecked")
				@Override
				public T get() {
					return (T) SpringInjector.this.beanFactory.getBean(name);
				}
			};
		}
		@SuppressWarnings("unchecked")
		final Class<T> cls = (Class<T>) type;
		return new Provider<T>() {
			@Override
			public T get() {
				return SpringInjector.this.beanFactory.getBean(cls);
			}
		};
	}

	private String extractName(Key<?> key) {
		if (key.getAnnotation() instanceof Named) {
			return ((Named) key.getAnnotation()).value();
		}
		return key.getTypeLiteral().getRawType().getSimpleName();
	}

	@Override
	public <T> Provider<T> getProvider(Class<T> type) {
		return getProvider(Key.get(type));
	}

	@Override
	public <T> T getInstance(Key<T> key) {
		return getProvider(key).get();
	}

	@Override
	public <T> T getInstance(Class<T> type) {
		return getInstance(Key.get(type));
	}

	@Override
	public Injector getParent() {
		return null;
	}

	@Override
	public Injector createChildInjector(Iterable<? extends Module> modules) {
		return null;
	}

	@Override
	public Injector createChildInjector(Module... modules) {
		return null;
	}

	@Override
	public Map<Class<? extends Annotation>, Scope> getScopeBindings() {
		return null;
	}

	@Override
	public Set<TypeConverterBinding> getTypeConverterBindings() {
		return null;
	}

}