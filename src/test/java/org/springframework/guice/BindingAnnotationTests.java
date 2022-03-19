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

import javax.inject.Named;
import javax.inject.Qualifier;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BindingAnnotationTests {

	@Test
	public void verifyBindingAnnotationsAreRespected() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BindingAnnotationTestsConfig.class);
		Injector injector = context.getBean(Injector.class);

		// Check @Qualifier
		SomeDependencyWithQualifierOnProvider someDependencyWithQualifierOnClass = injector
				.getInstance(Key.get(SomeDependencyWithQualifierOnProvider.class, SomeQualifierAnnotation.class));
		assertNotNull(someDependencyWithQualifierOnClass);

		// Check @BindingAnnotation on Spring @Bean available in Guice
		SomeDependencyWithQualifierOnProvider someDependencyWithBindingAnnotationOnProvider = injector
				.getInstance(Key.get(SomeDependencyWithQualifierOnProvider.class, SomeQualifierAnnotation.class));
		assertNotNull(someDependencyWithBindingAnnotationOnProvider);

		// Check @BindingAnnotation on Guice Binding available in Spring
		SomeStringHolder stringHolder = context.getBean(SomeStringHolder.class);
		assertEquals("annotated", stringHolder.annotatedString);
		assertEquals("other", stringHolder.otherAnnotatedString);

		// Check javax @Named
		SomeDependencyWithNamedAnnotationOnProvider someDependencyWithNamedAnnotationOnProvider = injector
				.getInstance(Key.get(SomeDependencyWithNamedAnnotationOnProvider.class, Names.named("javaxNamed")));
		assertNotNull(someDependencyWithNamedAnnotationOnProvider);

		// Check Guice @Named
		SomeDependencyWithGuiceNamedAnnotationOnProvider someDependencyWithGuiceNamedAnnotationOnProvider = injector
				.getInstance(
						Key.get(SomeDependencyWithGuiceNamedAnnotationOnProvider.class, Names.named("guiceNamed")));
		assertNotNull(someDependencyWithGuiceNamedAnnotationOnProvider);

		SomeDependencyWithGuiceNamedAnnotationOnProvider someSecondDependencyWithGuiceNamedAnnotationOnProvider = injector
				.getInstance(
						Key.get(SomeDependencyWithGuiceNamedAnnotationOnProvider.class, Names.named("guiceNamed2")));
		assertNotNull(someSecondDependencyWithGuiceNamedAnnotationOnProvider);

		// Check @Qualifier with Interface
		SomeInterface someInterface = injector.getInstance(Key.get(SomeInterface.class, SomeQualifierAnnotation.class));
		assertNotNull(someInterface);

		// Check different types with same @Named
		assertNotNull(injector.getInstance(SomeNamedDepWithType1.class));
		assertNotNull(injector.getInstance(SomeNamedDepWithType2.class));

		assertNotNull(
				injector.getInstance(Key.get(SomeNamedDepWithType1.class, Names.named("sameNameDifferentType"))));
		assertNotNull(
				injector.getInstance(Key.get(SomeNamedDepWithType2.class, Names.named("sameNameDifferentType"))));
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
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
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
		@Named("javaxNamed")
		SomeDependencyWithNamedAnnotationOnProvider someDependencyWithNamedAnnotationOnProvider() {
			return new SomeDependencyWithNamedAnnotationOnProvider();
		}

		@Bean(name = "javaxNamed2")
		@Named("javaxNamed2")
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
		AbstractModule module() {
			return new AbstractModule() {
				@Override
				protected void configure() {
					bind(String.class).annotatedWith(SomeBindingAnnotation.class).toInstance("annotated");
					bind(String.class).annotatedWith(SomeOtherBindingAnnotation.class).toInstance("other");
				}
			};
		}

	}

}
