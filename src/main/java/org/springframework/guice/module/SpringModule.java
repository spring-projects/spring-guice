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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.google.inject.spi.ProvisionListener;

/**
 * @author Dave Syer
 *
 */
public class SpringModule extends AbstractModule {

	private BindingTypeMatcher matcher = new GuiceModuleMetadata();

	private Map<StageTypeKey, Provider<?>> bound = new HashMap<StageTypeKey, Provider<?>>();

	private ConfigurableListableBeanFactory beanFactory;

	private Provider<ConfigurableListableBeanFactory> beanFactoryProvider;

	public SpringModule(ApplicationContext context) {
		this((ConfigurableListableBeanFactory) context.getAutowireCapableBeanFactory());
	}

	public SpringModule(ConfigurableListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	public SpringModule(Provider<ConfigurableListableBeanFactory> beanFactoryProvider) {
		this.beanFactoryProvider = beanFactoryProvider;
	}

	@Override
	public void configure() {
		if (binder().currentStage() != Stage.TOOL) {
			if (beanFactory == null) {
				beanFactory = beanFactoryProvider.get();
			}
			if (beanFactory.getBeanNamesForType(ProvisionListener.class).length > 0) {
				binder().bindListener(Matchers.any(),
						beanFactory.getBeansOfType(ProvisionListener.class).values()
								.toArray(new ProvisionListener[0]));
			}
			if (beanFactory instanceof DefaultListableBeanFactory) {
				((DefaultListableBeanFactory) beanFactory)
						.setAutowireCandidateResolver(new GuiceAutowireCandidateResolver(
								binder().getProvider(Injector.class)));
			}
			if (beanFactory.getBeanNamesForType(GuiceModuleMetadata.class).length > 0) {
				this.matcher = new CompositeTypeMatcher(
						beanFactory.getBeansOfType(GuiceModuleMetadata.class).values());
			}
		}
		bind(beanFactory);
	}

	private void bind(ConfigurableListableBeanFactory beanFactory) {
		for (String name : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition definition = beanFactory.getBeanDefinition(name);
			if(definition.hasAttribute("spring-guice")){
				continue;
			}
			if (definition.isAutowireCandidate()
					&& definition.getRole() == AbstractBeanDefinition.ROLE_APPLICATION) {
				Class<?> type = beanFactory.getType(name);
				if(type == null) 
				{
				    continue;
				}
				final String beanName = name;
				Provider<?> typeProvider = BeanFactoryProvider.typed(beanFactory, type);
				Provider<?> namedProvider = BeanFactoryProvider.named(beanFactory,
						beanName, type);
				if (!type.isInterface() && !ClassUtils.isCglibProxyClass(type)) {
					bindConditionally(binder(), name, type, typeProvider, namedProvider);
				}
				for (Class<?> iface : ClassUtils.getAllInterfacesForClass(type)) {
					bindConditionally(binder(), name, iface, typeProvider, namedProvider);
				}
				for (Type iface : type.getGenericInterfaces()) {
					bindConditionally(binder(), name, iface, typeProvider, namedProvider);
				}
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void bindConditionally(Binder binder, String name, Type type,
			Provider typeProvider, Provider namedProvider) {
		if (!this.matcher.matches(name, type)) {
			return;
		}
		if (type.getTypeName().startsWith("com.google.inject")) {
			return;
		}
		if (type instanceof ParameterizedType) {
			ParameterizedType param = (ParameterizedType) type;
			for (Type t : param.getActualTypeArguments()) {
				if (!ClassUtils.isPresent(t.getTypeName(), null)) {
					return;
				}
			}
		}
		StageTypeKey stageTypeKey = new StageTypeKey(binder.currentStage(), type);
		if (this.bound.get(stageTypeKey) == null) {
			// Only bind one provider for each type
			binder.withSource("spring-guice").bind(Key.get(type))
					.toProvider(typeProvider);
			this.bound.put(stageTypeKey, typeProvider);
		}
		// But allow binding to named beans
		binder.withSource("spring-guice").bind(TypeLiteral.get(type))
				.annotatedWith(Names.named(name)).toProvider(namedProvider);
	}
	
	private static class StageTypeKey {
		
		private final Stage stage;
		private final Type type;

		public StageTypeKey(Stage stage, Type type) {
			this.stage = stage;
			this.type = type;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((stage == null) ? 0 : stage.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			StageTypeKey other = (StageTypeKey) obj;
			if (stage != other.stage)
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}
	}

	private static class BeanFactoryProvider<T> implements Provider<T> {

		private ConfigurableListableBeanFactory beanFactory;

		private String name;

		private Class<T> type;

		private T result;

		private BeanFactoryProvider(ConfigurableListableBeanFactory beanFactory,
				String name, Class<T> type) {
			this.beanFactory = beanFactory;
			this.name = name;
			this.type = type;
		}

		public static <S> Provider<S> named(ConfigurableListableBeanFactory beanFactory,
				String name, Class<S> type) {
			return new BeanFactoryProvider<S>(beanFactory, name, type);
		}

		public static <S> Provider<S> typed(ConfigurableListableBeanFactory beanFactory,
				Class<S> type) {
			return new BeanFactoryProvider<S>(beanFactory, null, type);
		}

		@Override
		public T get() {
			if (this.result == null) {

				String[] named = BeanFactoryUtils
						.beanNamesForTypeIncludingAncestors(this.beanFactory, this.type);
				List<String> names = new ArrayList<String>(named.length);
				if (named.length == 1) {
					names.add(named[0]);
				}
				else {
					for (String name : named) {
						if (name.equals(this.name))
							names.add(name);
					}
				}
				if (names.size() == 1) {
					this.result = this.beanFactory.getBean(names.get(0), this.type);
				}
				else {
					for (String name : named) {
						if (this.beanFactory.getBeanDefinition(name).isPrimary()) {
							this.result = this.beanFactory.getBean(name, this.type);
							break;
						}
					}
					if (this.result == null) {
						throw new ProvisionException(
								"No primary bean definition for type: " + this.type);
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
