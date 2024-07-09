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

package org.springframework.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.guice.module.BeanFactoryProvider;
import org.springframework.guice.module.SpringModule;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

public class PartialInjectionTests {

	@Test
	void shouldResolveNamedComponentsInSpringWhenUsingSetterInjection() {
		Injector injector = guiceInjectorWithSpringBean(SetterInjectionExample.class);

		SetterInjectionExample example = injector.getInstance(SetterInjectionExample.class);

		assertThat(example.getNamedMessage()).isEqualTo("banana");
	}

	@Test
	void shouldResolveNamedComponentsInSpringWhenUsingConstructorInjection() {
		Injector injector = guiceInjectorWithSpringBean(ConstructorInjectionExample.class);

		ConstructorInjectionExample example = injector.getInstance(ConstructorInjectionExample.class);

		assertThat(example.getNamedMessage()).isEqualTo("banana");
	}

	@Test
	void shouldResolveComponentsInSpringWhenUsingSetterInjection() {
		Injector injector = guiceInjectorWithSpringBean(SetterInjectionExample.class);

		SetterInjectionExample example = injector.getInstance(SetterInjectionExample.class);

		assertThat(example.getUnnamedMessage()).isEqualTo("apple");
	}

	@Test
	void shouldResolveComponentsInSpringWhenUsingConstructorInjection() {
		Injector injector = guiceInjectorWithSpringBean(ConstructorInjectionExample.class);

		ConstructorInjectionExample example = injector.getInstance(ConstructorInjectionExample.class);

		assertThat(example.getUnnamedMessage()).isEqualTo("apple");
	}

	@Test
	void shouldResolveNamedComponentOnSecondInjectWhenUsingConstructorInjection() {
		Injector injector = guiceInjectorWithSpringBean(ConstructorInjectionExample.class);

		ConstructorInjectionExample example = injector.getInstance(ConstructorInjectionExample.class);

		example.getNamedMessage();
		assertThat(example.getNamedMessage()).isEqualTo("banana");
	}

	@Test
	void shouldResolveNamedComponentOnSecondInjectWhenUsingSetterInjection() {
		Injector injector = guiceInjectorWithSpringBean(SetterInjectionExample.class);

		SetterInjectionExample example = injector.getInstance(SetterInjectionExample.class);

		example.getNamedMessage();
		assertThat(example.getNamedMessage()).isEqualTo("banana");
	}

	private Injector guiceInjectorWithSpringBean(Class<?> classForContext) {
		Class<?>[] components = new Class<?>[] { classForContext };
		BeanFactoryProvider beanFactoryProvider = BeanFactoryProvider.from(components);
		return Guice.createInjector(new SpringModule(beanFactoryProvider), new ExampleGuiceModule());
	}

	@Component
	public static class SetterInjectionExample {

		@Autowired
		@Qualifier("named")
		private Dependency named;

		@Autowired
		private Dependency unnamed;

		public String getNamedMessage() {
			return this.named.getMessage();
		}

		public String getUnnamedMessage() {
			return this.unnamed.getMessage();
		}

	}

	@Component
	public static class ConstructorInjectionExample {

		private final Dependency named;

		private final Dependency unnamed;

		@Autowired
		public ConstructorInjectionExample(@Qualifier("named") Dependency named, Dependency unnamed) {
			this.named = named;
			this.unnamed = unnamed;
		}

		public String getNamedMessage() {
			return this.named.getMessage();
		}

		public String getUnnamedMessage() {
			return this.unnamed.getMessage();
		}

	}

	public static class ExampleGuiceModule extends AbstractModule {

		@Provides
		@Singleton
		@Named("named")
		public Dependency namedDependencyProvider() {
			return new Dependency("banana");
		}

		@Provides
		@Singleton
		public Dependency unnamedDependencyProvider() {
			return new Dependency("apple");
		}

	}

	public static class Dependency {

		private final String message;

		public Dependency(String message) {
			this.message = message;
		}

		public String getMessage() {
			return this.message;
		}

	}

}
