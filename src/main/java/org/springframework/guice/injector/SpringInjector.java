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
import com.google.inject.spi.TypeConverterBinding;

public class SpringInjector implements Injector {
	
	private Injector injector;
	private DefaultListableBeanFactory beanFactory;
	
	public SpringInjector(ApplicationContext context) {
		this.beanFactory = (DefaultListableBeanFactory) context.getAutowireCapableBeanFactory();
		if (context.getBeanNamesForType(Injector.class).length>0) {
			this.injector = context.getBean(Injector.class);
		}
	}

	@Override
	public void injectMembers(Object instance) {
		beanFactory.autowireBean(instance);
	}

	@Override
	public <T> MembersInjector<T> getMembersInjector(TypeLiteral<T> typeLiteral) {
		return new MembersInjector<T>() {
			@Override
			public void injectMembers(T instance) {
				beanFactory.autowireBean(instance);
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
		@SuppressWarnings("unchecked")
		Provider<T> provider = (Provider<T>) getProvider(key.getTypeLiteral().getRawType());
		return provider;
	}

	@Override
	public <T> Provider<T> getProvider(Class<T> type) {
		if (beanFactory.getBeanNamesForType(type).length==0) {
			if (injector!=null && injector.getExistingBinding(Key.get(type))!=null) {
				return injector.getProvider(type);
			}
			// TODO: use prototype scope?
			beanFactory.registerBeanDefinition(type.getSimpleName(), new RootBeanDefinition(type));
		}
		final Class<T> cls = type;
		return new Provider<T>() {
			@Override
			public T get() {
				return beanFactory.getBean(cls);
			}
		};
	}

	@Override
	public <T> T getInstance(Key<T> key) {
		// TODO: support for other metadata in the key
		@SuppressWarnings("unchecked")
		T provider = (T) getInstance(key.getTypeLiteral().getRawType());
		return provider;
	}

	@Override
	public <T> T getInstance(Class<T> type) {
		if (beanFactory.getBeanNamesForType(type).length==0) {
			if (injector!=null && injector.getExistingBinding(Key.get(type))!=null) {
				return injector.getInstance(type);
			}
			// TODO: use prototype scope?
			beanFactory.registerBeanDefinition(type.getSimpleName(), new RootBeanDefinition(type));
		}
		return beanFactory.getBean(type);
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