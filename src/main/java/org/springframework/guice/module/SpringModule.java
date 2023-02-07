/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.guice.module;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

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
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.spi.ProvisionListener;
import com.google.inject.util.Types;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.ClassUtils;

/**
 * A Guice module that wraps a Spring {@link ApplicationContext}.
 *
 * @author Dave Syer
 *
 */
public class SpringModule extends AbstractModule {

	/**
	 * Identifier for bindings provided by this module.
	 */
	public static final String SPRING_GUICE_SOURCE = "spring-guice";

	private BindingTypeMatcher matcher = new GuiceModuleMetadata();

	private Map<StageTypeKey, Provider<?>> bound = new HashMap<StageTypeKey, Provider<?>>();

	private ConfigurableListableBeanFactory beanFactory;

	private Provider<ConfigurableListableBeanFactory> beanFactoryProvider;

	private boolean enableJustInTimeBinding = true;

	private Provider<Injector> injector;

	public SpringModule(ApplicationContext context) {
		this(context, true);
	}

	public SpringModule(ApplicationContext context, boolean enableJustInTimeBinding) {
		this((ConfigurableListableBeanFactory) context.getAutowireCapableBeanFactory(), enableJustInTimeBinding);
	}

	public SpringModule(ConfigurableListableBeanFactory beanFactory) {
		this(beanFactory, true);
	}

	public SpringModule(ConfigurableListableBeanFactory beanFactory, boolean enableJustInTimeBinding) {
		this.beanFactory = beanFactory;
		this.enableJustInTimeBinding = enableJustInTimeBinding;
	}

	public SpringModule(Provider<ConfigurableListableBeanFactory> beanFactoryProvider) {
		this.beanFactoryProvider = beanFactoryProvider;
	}

	@Override
	public void configure() {
		if (this.beanFactory == null) {
			this.beanFactory = this.beanFactoryProvider.get();
		}
		this.injector = binder().getProvider(Injector.class);
		if (this.beanFactory.getBeanNamesForType(ProvisionListener.class).length > 0) {
			binder().bindListener(Matchers.any(), this.beanFactory.getBeansOfType(ProvisionListener.class).values()
					.toArray(new ProvisionListener[0]));
		}
		if (this.enableJustInTimeBinding) {
			if (this.beanFactory instanceof DefaultListableBeanFactory) {
				((DefaultListableBeanFactory) this.beanFactory)
						.setAutowireCandidateResolver(new GuiceAutowireCandidateResolver(this.injector));
			}
		}
		if (this.beanFactory.getBeanNamesForType(GuiceModuleMetadata.class).length > 0) {
			this.matcher = new CompositeTypeMatcher(
					this.beanFactory.getBeansOfType(GuiceModuleMetadata.class).values());
		}
		bind(this.beanFactory);
	}

	public Provider<Injector> getInjector() {
		return this.injector;
	}

	private void bind(ConfigurableListableBeanFactory beanFactory) {
		for (String name : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition definition = beanFactory.getBeanDefinition(name);

			if (definition.hasAttribute(SPRING_GUICE_SOURCE)) {
				continue;
			}
			Optional<Annotation> bindingAnnotation = getAnnotationForBeanDefinition(definition);
			if (definition.isAutowireCandidate() && definition.getRole() == AbstractBeanDefinition.ROLE_APPLICATION) {
				Type type;
				Class<?> clazz = beanFactory.getType(name);
				if (clazz == null) {
					continue;
				}
				if (clazz.getTypeParameters().length > 0) {
					RootBeanDefinition rootBeanDefinition = (RootBeanDefinition) beanFactory
							.getMergedBeanDefinition(name);
					if (rootBeanDefinition.getFactoryBeanName() != null
							&& rootBeanDefinition.getResolvedFactoryMethod() != null) {
						type = rootBeanDefinition.getResolvedFactoryMethod().getGenericReturnType();
					}
					else {
						type = rootBeanDefinition.getResolvableType().getType();
					}
					if (type instanceof ParameterizedType) {
						ParameterizedType parameterizedType = (ParameterizedType) type;
						if (parameterizedType.getRawType() instanceof Class
								&& FactoryBean.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
							type = Types.newParameterizedTypeWithOwner(parameterizedType.getOwnerType(), clazz,
									parameterizedType.getActualTypeArguments());
						}
					}

				}
				else {
					type = clazz;
				}

				Provider<?> typeProvider = BeanFactoryProvider.typed(beanFactory, type, bindingAnnotation);
				Provider<?> namedProvider = BeanFactoryProvider.named(beanFactory, name, type, bindingAnnotation);

				if (!clazz.isInterface() && !clazz.getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) {
					bindConditionally(binder(), name, clazz, typeProvider, namedProvider, bindingAnnotation);
				}
				for (Type superType : getAllSuperTypes(type, clazz)) {
					if (!superType.getTypeName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)
							&& !superType.equals(Object.class)) {
						bindConditionally(binder(), name, superType, typeProvider, namedProvider, bindingAnnotation);
					}
				}
				for (Type iface : clazz.getGenericInterfaces()) {
					bindConditionally(binder(), name, iface, typeProvider, namedProvider, bindingAnnotation);
				}
			}
		}
	}

	private static String getNameFromBindingAnnotation(Optional<Annotation> bindingAnnotation) {
		if (bindingAnnotation.isPresent()) {
			Annotation annotation = bindingAnnotation.get();
			if (annotation instanceof Named) {
				return ((Named) annotation).value();
			}
			else if (annotation instanceof javax.inject.Named) {
				return ((javax.inject.Named) annotation).value();
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
	}

	private static Optional<Annotation> getAnnotationForBeanDefinition(BeanDefinition definition) {
		if (definition instanceof AnnotatedBeanDefinition) {
			MethodMetadata methodMetadata = ((AnnotatedBeanDefinition) definition).getFactoryMethodMetadata();
			if (methodMetadata != null) {
				return methodMetadata.getAnnotations().stream().filter(MergedAnnotation::isDirectlyPresent)
						.filter((mergedAnnotation) -> Annotations.isBindingAnnotation(mergedAnnotation.getType()))
						.map(MergedAnnotation::synthesize).findFirst();
			}
			else {
				return Optional.empty();
			}
		}
		else {
			return Optional.empty();
		}
	}

	private static Set<Type> getAllSuperTypes(Type originalType, Class<?> clazz) {
		Set<Type> allInterfaces = new HashSet<>();
		TypeLiteral<?> typeToken = TypeLiteral.get(originalType);
		Queue<Type> queue = new LinkedList<>();
		queue.add(clazz);
		if (originalType != clazz) {
			queue.add(originalType);
		}
		while (!queue.isEmpty()) {
			Type type = queue.poll();
			allInterfaces.add(type);
			if (type instanceof Class) {
				for (Type i : ((Class<?>) type).getInterfaces()) {
					if (i instanceof Class && ((Class<?>) i).isAssignableFrom(typeToken.getRawType())) {
						Type superInterface = typeToken.getSupertype((Class<?>) i).getType();
						queue.add(superInterface);
						if (!(superInterface instanceof Class)) {
							queue.add(i);
						}
					}
				}
				if (((Class<?>) type).getSuperclass() != null
						&& ((Class<?>) type).isAssignableFrom(typeToken.getRawType())) {
					Type superClass = typeToken.getSupertype(((Class<?>) type).getSuperclass()).getType();
					queue.add(superClass);
				}
			}
		}
		return allInterfaces;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void bindConditionally(Binder binder, String name, Type type, Provider typeProvider, Provider namedProvider,
			Optional<Annotation> bindingAnnotation) {
		if (!this.matcher.matches(name, type)) {
			return;
		}
		String typeName = type.getTypeName();
		if (typeName.startsWith("com.google.inject") || typeName.startsWith("javax.inject.Provider")) {
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
		Key<?> key = bindingAnnotation.map((a) -> (Key<Object>) Key.get(type, a)).orElse((Key<Object>) Key.get(type));
		StageTypeKey stageTypeKey = new StageTypeKey(binder.currentStage(), key);
		// Only bind one provider for each type
		if (this.bound.put(stageTypeKey, typeProvider) == null) {
			binder.withSource(SPRING_GUICE_SOURCE).bind(key).toProvider(typeProvider);
		}
		// Allow binding to named beans if not already bound
		if (!name.equals(getNameFromBindingAnnotation(bindingAnnotation))) {
			binder.withSource(SPRING_GUICE_SOURCE).bind(TypeLiteral.get(type)).annotatedWith(Names.named(name))
					.toProvider(namedProvider);
		}
	}

	private static class StageTypeKey {

		private final Stage stage;

		private Key<?> key;

		StageTypeKey(Stage stage, Key<?> key) {
			this.stage = stage;
			this.key = key;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			StageTypeKey other = (StageTypeKey) obj;
			if (this.key == null) {
				if (other.key != null) {
					return false;
				}
			}
			else if (!this.key.equals(other.key)) {
				return false;
			}
			if (this.stage != other.stage) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((this.key == null) ? 0 : this.key.hashCode());
			result = prime * result + ((this.stage == null) ? 0 : this.stage.hashCode());
			return result;
		}

		@Override
		public String toString() {
			return "StageTypeKey[key=" + this.key + ", stage=" + this.stage + "]";
		}

	}

	@SuppressWarnings("checkstyle:FinalClass")
	private static class BeanFactoryProvider implements Provider<Object> {

		private ConfigurableListableBeanFactory beanFactory;

		private String name;

		private Type type;

		private Provider<Object> resultProvider;

		private Optional<Annotation> bindingAnnotation;

		private BeanFactoryProvider(ConfigurableListableBeanFactory beanFactory, String name, Type type,
				Optional<Annotation> bindingAnnotation) {
			this.beanFactory = beanFactory;
			this.name = name;
			this.bindingAnnotation = bindingAnnotation;
			this.type = type;
		}

		@SuppressWarnings("checkstyle:SpringMethodVisibility")
		public static Provider<?> named(ConfigurableListableBeanFactory beanFactory, String name, Type type,
				Optional<Annotation> bindingAnnotation) {
			return new BeanFactoryProvider(beanFactory, name, type, bindingAnnotation);
		}

		@SuppressWarnings("checkstyle:SpringMethodVisibility")
		public static Provider<?> typed(ConfigurableListableBeanFactory beanFactory, Type type,
				Optional<Annotation> bindingAnnotation) {
			return new BeanFactoryProvider(beanFactory, null, type, bindingAnnotation);
		}

		@Override
		@SuppressWarnings("checkstyle:NestedIfDepth")
		public Object get() {
			if (this.resultProvider == null) {

				String[] named = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory,
						ResolvableType.forType(this.type));

				List<String> candidateBeanNames = new ArrayList<>(named.length);
				for (String name : named) {
					BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition(name);
					// This is a Guice component bridged to spring
					// If this were the target candidate,
					// Guice would have injected it natively.
					// Thus, it cannot be a candidate.
					// GuiceFactoryBeans don't have 1-to-1 annotation mapping
					// (since annotation attributes are ignored)
					// Skip this candidate to avoid unexpected matches
					// due to imprecise annotation mapping
					if (!beanDefinition.hasAttribute(SPRING_GUICE_SOURCE)) {
						candidateBeanNames.add(name);
					}
				}

				List<String> matchingBeanNames;
				if (candidateBeanNames.size() == 1) {
					matchingBeanNames = candidateBeanNames;
				}
				else {
					matchingBeanNames = new ArrayList<String>(candidateBeanNames.size());
					for (String name : candidateBeanNames) {
						// Make sure we don't add the same name twice using if/else
						if (name.equals(this.name)) {
							// Guice is injecting dependency of this type by bean name
							matchingBeanNames.add(name);
						}
						else if (this.bindingAnnotation.isPresent()) {
							String boundName = getNameFromBindingAnnotation(this.bindingAnnotation);
							if (name.equals(boundName)) {
								// Spring bean definition has a Named annotation that
								// matches the name of the bean
								// In such cases, we dedupe namedProvider (because it's
								// Key equals typeProvider Key)
								// Thus, this complementary check is required
								// (because name field is null in typeProvider,
								// and if check above wouldn't pass)
								matchingBeanNames.add(name);
							}
							else {
								Optional<Annotation> annotationOptional = SpringModule
										.getAnnotationForBeanDefinition(this.beanFactory.getBeanDefinition(name));

								if (annotationOptional.equals(this.bindingAnnotation)) {
									// Found a bean with matching qualifier annotation
									matchingBeanNames.add(name);
								}
							}
						}
					}
				}
				if (matchingBeanNames.size() == 1) {
					this.resultProvider = () -> this.beanFactory.getBean(matchingBeanNames.get(0));
				}
				else {
					// Shouldn't we iterate over matching bean names here?
					for (String name : candidateBeanNames) {
						if (this.beanFactory.getBeanDefinition(name).isPrimary()) {
							this.resultProvider = () -> this.beanFactory.getBean(name);
							break;
						}
					}
					if (this.resultProvider == null) {
						throw new ProvisionException("No primary bean definition for type: " + this.type);
					}
				}
			}
			return this.resultProvider.get();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof BeanFactoryProvider) {
				BeanFactoryProvider o = (BeanFactoryProvider) obj;
				return ((this.name == null && o.name == null) || (this.name != null && this.name.equals(o.name)))
						&& this.type.equals(o.type);
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.name, this.type);
		}

	}

	private static class CompositeTypeMatcher implements BindingTypeMatcher {

		private Collection<? extends BindingTypeMatcher> matchers;

		CompositeTypeMatcher(Collection<? extends BindingTypeMatcher> matchers) {
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
