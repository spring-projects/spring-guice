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

package demo;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;

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
	
	private GenericApplicationContext context;
	private Injector injector;
	
	public SpringInjector(GenericApplicationContext context) {
		this.context = context;
		if (context.getBeanNamesForType(Injector.class).length>0) {
			injector = context.getBean(Injector.class);
		}
	}

	@Override
	public void injectMembers(Object instance) {
	
	}

	@Override
	public <T> MembersInjector<T> getMembersInjector(TypeLiteral<T> typeLiteral) {
		return null;
	}

	@Override
	public <T> MembersInjector<T> getMembersInjector(Class<T> type) {
		return null;
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
		return null;
	}

	@Override
	public <T> Provider<T> getProvider(Class<T> type) {
		return null;
	}

	@Override
	public <T> T getInstance(Key<T> key) {
		return null;
	}

	@Override
	public <T> T getInstance(Class<T> type) {
		if (context.getBeanNamesForType(type).length==0) {
			if (injector!=null && injector.getExistingBinding(Key.get(type))!=null) {
				return injector.getInstance(type);
			}
			// TODO: use prototype scope?
			context.getDefaultListableBeanFactory().registerBeanDefinition(type.getSimpleName(), new RootBeanDefinition(type));
		}
		return context.getBean(type);
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