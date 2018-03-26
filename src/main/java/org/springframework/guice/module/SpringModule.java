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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Provider;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.internal.Annotations;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.google.inject.spi.ProvisionListener;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Dave Syer
 *
 */
public class SpringModule extends AbstractModule {

	public static final String SPRING_GUICE_SOURCE = "spring-guice";

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
		bind(beanFactory);
	}

	private void bind(ConfigurableListableBeanFactory beanFactory) {
		for (String name : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition definition = beanFactory.getBeanDefinition(name);
			
			if (definition.hasAttribute(SPRING_GUICE_SOURCE)) {
				continue;
			}
			Optional<Annotation> bindingAnnotation = getAnnotationForBeanDefinition(definition);
			if (definition.isAutowireCandidate()
					&& definition.getRole() == AbstractBeanDefinition.ROLE_APPLICATION) {
				Class<?> type = beanFactory.getType(name);
				if (type == null) {
					continue;
				}
				final String beanName = name;
				Provider<?> typeProvider = BeanFactoryProvider.typed(beanFactory, type);
				Provider<?> namedProvider = BeanFactoryProvider.named(beanFactory,
						beanName, type);
				if (!type.isInterface() && !ClassUtils.isCglibProxyClass(type)) {
					bindConditionally(binder(), name, type, typeProvider, namedProvider, bindingAnnotation);
				}
				for (Class<?> iface : ClassUtils.getAllInterfacesForClass(type)) {
					bindConditionally(binder(), name, iface, typeProvider, namedProvider, bindingAnnotation);
				}
				for (Type iface : type.getGenericInterfaces()) {
					bindConditionally(binder(), name, iface, typeProvider, namedProvider, bindingAnnotation);
				}
			}
		}
	}

	private Optional<Annotation> getAnnotationForBeanDefinition(BeanDefinition definition) {
		if (definition instanceof AnnotatedBeanDefinition
				&& ((AnnotatedBeanDefinition) definition).getFactoryMethodMetadata() != null) {
			try {
				Method factoryMethod = getFactoryMethod(beanFactory, definition);
				return Arrays.stream(AnnotationUtils.getAnnotations(factoryMethod))
						.filter(a -> Annotations.isBindingAnnotation(a.annotationType())).findFirst();
			} catch (Exception e) {
				return Optional.empty();
			}
		} else {
			return Optional.empty();
		}
	}

	private Method getFactoryMethod(ConfigurableListableBeanFactory beanFactory,
			BeanDefinition definition) throws Exception {
		if (definition instanceof AnnotatedBeanDefinition) {
			MethodMetadata factoryMethodMetadata = ((AnnotatedBeanDefinition) definition)
					.getFactoryMethodMetadata();
			if (factoryMethodMetadata instanceof StandardMethodMetadata) {
				return ((StandardMethodMetadata) factoryMethodMetadata)
						.getIntrospectedMethod();
			}
		}
		BeanDefinition factoryDefinition = beanFactory
				.getBeanDefinition(definition.getFactoryBeanName());
		Class<?> factoryClass = ClassUtils.forName(factoryDefinition.getBeanClassName(),
				beanFactory.getBeanClassLoader());
		return getFactoryMethod(definition, factoryClass);
	}

	private Method getFactoryMethod(BeanDefinition definition, Class<?> factoryClass) {
		Method uniqueMethod = null;
		for (Method candidate : getCandidateFactoryMethods(definition, factoryClass)) {
			if (candidate.getName().equals(definition.getFactoryMethodName())) {
				if (uniqueMethod == null) {
					uniqueMethod = candidate;
				}
				else if (!hasMatchingParameterTypes(candidate, uniqueMethod)) {
					return null;
				}
			}
		}
		return uniqueMethod;
	}

	private Method[] getCandidateFactoryMethods(BeanDefinition definition,
			Class<?> factoryClass) {
		return shouldConsiderNonPublicMethods(definition)
				? ReflectionUtils.getAllDeclaredMethods(factoryClass)
				: factoryClass.getMethods();
	}

	private boolean shouldConsiderNonPublicMethods(BeanDefinition definition) {
		return (definition instanceof AbstractBeanDefinition)
				&& ((AbstractBeanDefinition) definition).isNonPublicAccessAllowed();
	}

	private boolean hasMatchingParameterTypes(Method candidate, Method current) {
		return Arrays.equals(candidate.getParameterTypes(), current.getParameterTypes());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void bindConditionally(Binder binder, String name, Type type,
			Provider typeProvider, Provider namedProvider, Optional<Annotation> bindingAnnotation) {
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
			Key<Object> key = bindingAnnotation.map(a ->(Key<Object>)Key.get(type, a)).orElse((Key<Object>)Key.get(type));
			binder.withSource(SPRING_GUICE_SOURCE).bind(key)
					.toProvider(typeProvider);
			this.bound.put(stageTypeKey, typeProvider);
		}
		// But allow binding to named beans
		binder.withSource(SPRING_GUICE_SOURCE).bind(TypeLiteral.get(type))
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
			}
			else if (!type.equals(other.type))
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
