/*
 * Copyright 2018-2022 the original author or authors.
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

package org.springframework.guice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.throwingproviders.CheckedProvider;
import com.google.inject.throwingproviders.CheckedProvides;
import com.google.inject.throwingproviders.ThrowingProviderBinder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class BindingAnnotationTests {

	@Test
	public void verifyBindingAnnotationsAreRespected() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BindingAnnotationTestsConfig.class);
		Injector injector = context.getBean(Injector.class);

		// Check @Qualifier
		SomeDependencyWithQualifierOnProvider someDependencyWithQualifierOnClass = injector
				.getInstance(Key.get(SomeDependencyWithQualifierOnProvider.class, SomeQualifierAnnotation.class));
		assertThat(someDependencyWithQualifierOnClass).isNotNull();

		// Check @BindingAnnotation on Spring @Bean available in Guice
		SomeDependencyWithQualifierOnProvider someDependencyWithBindingAnnotationOnProvider = injector
				.getInstance(Key.get(SomeDependencyWithQualifierOnProvider.class, SomeQualifierAnnotation.class));
		assertThat(someDependencyWithBindingAnnotationOnProvider).isNotNull();

		// Check @BindingAnnotation on Guice Binding available in Spring
		SomeStringHolder stringHolder = context.getBean(SomeStringHolder.class);
		assertThat(stringHolder.annotatedString).isEqualTo("annotated");
		assertThat(stringHolder.otherAnnotatedString).isEqualTo("other");

		// Check jakarta @Named
		SomeDependencyWithNamedAnnotationOnProvider someDependencyWithNamedAnnotationOnProvider = injector
				.getInstance(Key.get(SomeDependencyWithNamedAnnotationOnProvider.class, Names.named("jakartaNamed")));
		assertThat(someDependencyWithNamedAnnotationOnProvider).isNotNull();

		// Check Guice @Named
		SomeDependencyWithGuiceNamedAnnotationOnProvider someDependencyWithGuiceNamedAnnotationOnProvider = injector
				.getInstance(
						Key.get(SomeDependencyWithGuiceNamedAnnotationOnProvider.class, Names.named("guiceNamed")));
		assertThat(someDependencyWithGuiceNamedAnnotationOnProvider).isNotNull();

		SomeDependencyWithGuiceNamedAnnotationOnProvider someSecondDependencyWithGuiceNamedAnnotationOnProvider = injector
				.getInstance(
						Key.get(SomeDependencyWithGuiceNamedAnnotationOnProvider.class, Names.named("guiceNamed2")));
		assertThat(someSecondDependencyWithGuiceNamedAnnotationOnProvider).isNotNull();

		// Check @Qualifier with Interface
		SomeInterface someInterface = injector.getInstance(Key.get(SomeInterface.class, SomeQualifierAnnotation.class));
		assertThat(someInterface).isNotNull();

		// Check different types with same @Named
		assertThat(injector.getInstance(SomeNamedDepWithType1.class)).isNotNull();
		assertThat(injector.getInstance(SomeNamedDepWithType2.class)).isNotNull();

		assertThat(injector.getInstance(Key.get(SomeNamedDepWithType1.class, Names.named("sameNameDifferentType"))))
				.isNotNull();
		assertThat(injector.getInstance(Key.get(SomeNamedDepWithType2.class, Names.named("sameNameDifferentType"))))
				.isNotNull();

		assertThat(injector
				.getInstance(Key.get(new TypeLiteral<TestCheckedProvider<SomeDependencyFromTestCheckedProvider>>() {
				}))).isNotNull();
		assertThat(injector.getInstance(
				Key.get(new TypeLiteral<TestCheckedProvider<SomeOtherDependencyFromTestCheckedProvider>>() {
				}))).isNotNull();

		context.close();
	}

	@Test
	public void verifyBindingAnnotationsDuplicateBeans() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BindingAnnotationTestsConfig.class);
		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class)
				.isThrownBy(() -> assertThat(context.getBean(SomeService.class)).isNotNull());
		context.close();
	}

	public static class SomeDependencyWithQualifierOnProvider {

	}

	public static class SomeDependencyWithBindingAnnotationOnProvider {

	}

	public static class SomeDependencyWithNamedAnnotationOnProvider {

	}

	public static class SomeDependencyWithGuiceNamedAnnotationOnProvider {

	}

	public interface SomeInterface {

	}

	public static class SomeDependencyWithQualifierOnProviderWhichImplementsSomeInterface implements SomeInterface {

	}

	public static class SomeNamedDepWithType1 {

	}

	public static class SomeNamedDepWithType2 {

	}

	@Qualifier
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface SomeQualifierAnnotation {

	}

	@BindingAnnotation
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
	@Retention(RetentionPolicy.RUNTIME)
	@interface SomeBindingAnnotation {

	}

	@BindingAnnotation
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface SomeOtherBindingAnnotation {

	}

	static class SomeStringHolder {

		@Autowired
		@SomeBindingAnnotation
		public String annotatedString;

		@Autowired
		@SomeOtherBindingAnnotation
		String otherAnnotatedString;

	}

	public interface TestCheckedProvider<T> extends CheckedProvider<T> {

	}

	public static class SomeDependencyFromTestCheckedProvider {

	}

	public static class SomeOtherDependencyFromTestCheckedProvider {

	}

	interface SomeService {

	}

	static class BaseSomeService implements SomeService {

	}

	static class ShadowingSomeService implements SomeService {

		@Inject
		ShadowingSomeService(@SomeBindingAnnotation SomeService baseService) {

		}

	}

	public static class SomeProvider implements jakarta.inject.Provider<Object> {

		@Override
		public Object get() {
			return null;
		}

	}

	@EnableGuiceModules
	@Configuration
	static class BindingAnnotationTestsConfig {

		@Bean
		@SomeQualifierAnnotation
		SomeDependencyWithQualifierOnProvider someDependencyWithQualifierOnProvider() {
			return new SomeDependencyWithQualifierOnProvider();
		}

		@Bean
		@SomeBindingAnnotation
		SomeDependencyWithBindingAnnotationOnProvider someDependencyWithBindingAnnotationOnProvider() {
			return new SomeDependencyWithBindingAnnotationOnProvider();
		}

		@Bean
		@Named("jakartaNamed")
		SomeDependencyWithNamedAnnotationOnProvider someDependencyWithNamedAnnotationOnProvider() {
			return new SomeDependencyWithNamedAnnotationOnProvider();
		}

		@Bean(name = "jakartaNamed2")
		@Named("jakartaNamed2")
		SomeDependencyWithNamedAnnotationOnProvider someSecondDependencyWithNamedAnnotationOnProvider() {
			return new SomeDependencyWithNamedAnnotationOnProvider();
		}

		@Bean
		@com.google.inject.name.Named("guiceNamed")
		SomeDependencyWithGuiceNamedAnnotationOnProvider someDependencyWithGuiceNamedAnnotationOnProvider() {
			return new SomeDependencyWithGuiceNamedAnnotationOnProvider();
		}

		@Bean
		@com.google.inject.name.Named("guiceNamed2")
		SomeDependencyWithGuiceNamedAnnotationOnProvider someSecondDependencyWithGuiceNamedAnnotationOnProvider() {
			return new SomeDependencyWithGuiceNamedAnnotationOnProvider();
		}

		@Bean
		@SomeQualifierAnnotation
		SomeInterface someInterface() {
			return new SomeDependencyWithQualifierOnProviderWhichImplementsSomeInterface();
		}

		@Bean
		@Named("sameNameDifferentType")
		SomeNamedDepWithType1 someNamedDepWithType1() {
			return new SomeNamedDepWithType1();
		}

		@Bean
		@Named("sameNameDifferentType")
		SomeNamedDepWithType2 someNamedDepWithType2() {
			return new SomeNamedDepWithType2();
		}

		@Bean
		SomeStringHolder stringHolder() {
			return new SomeStringHolder();
		}

		@Bean
		SomeProvider someProvider() {
			return new SomeProvider();
		}

		@Bean
		static AbstractModule module() {
			return new AbstractModule() {
				@Override
				protected void configure() {
					install(ThrowingProviderBinder.forModule(this));
					bind(String.class).annotatedWith(SomeBindingAnnotation.class).toInstance("annotated");
					bind(String.class).annotatedWith(SomeOtherBindingAnnotation.class).toInstance("other");

					bind(SomeService.class).annotatedWith(SomeBindingAnnotation.class).to(BaseSomeService.class);
					bind(SomeService.class).to(ShadowingSomeService.class);
				}

				@CheckedProvides(TestCheckedProvider.class)
				SomeDependencyFromTestCheckedProvider some() throws Exception {
					return new SomeDependencyFromTestCheckedProvider();
				}

				@CheckedProvides(TestCheckedProvider.class)
				SomeOtherDependencyFromTestCheckedProvider other() throws Exception {
					return new SomeOtherDependencyFromTestCheckedProvider();
				}

			};
		}

	}

}
