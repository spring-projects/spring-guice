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

import java.util.function.Supplier;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;
import org.springframework.guice.injector.SpringInjector;

/**
 * Test Generics (e.g., {@literal Supplier<T>}) not losing type info across bridge in both
 * directions
 *
 * @author Howard Yuan
 */
public class ProvidesSupplierWiringTests {

	// Test Guice -> Spring direction
	@SuppressWarnings({ "resource", "unused" })
	@Test
	public void testProvidesSupplier() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ModulesConfig.class,
				FooBar.class);
		Foo foo = context.getBean(Foo.class);
		Bar bar = context.getBean(Bar.class);
	}

	// Test Spring -> Guice direction
	// ToDo -- Today this direction doesn't work without further work. Ignore the test for
	// now.
	@SuppressWarnings("unused")
	@Disabled
	@Test
	public void testProvidesSupplierSpring() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(FooBarSpring.class);
		SpringInjector injector = new SpringInjector(context);
		Foo_Spring fooSpring = injector.getInstance(Key.get(new TypeLiteral<Supplier<Foo_Spring>>() {
		})).get();
		Bar_Spring barSpring = injector.getInstance(Key.get(new TypeLiteral<Supplier<Bar_Spring>>() {
		})).get();
	}

	@Configuration
	@EnableGuiceModules
	static class ModulesConfig {

		@Bean
		TestConfig testConfig() {
			return new TestConfig();
		}

	}

	@Configuration
	static class FooBar {

		@Bean
		Foo foo(Supplier<Foo> supplier) {
			return supplier.get();
		}

		@Bean
		Bar bar(Supplier<Bar> supplier) {
			return supplier.get();
		}

	}

	static class TestConfig extends AbstractModule {

		@Override
		protected void configure() {
		}

		@Singleton
		@Provides
		Supplier<Foo> getFoo() {
			return () -> new Foo();
		}

		@Singleton
		@Provides
		Supplier<Bar> getBar() {
			return () -> new Bar();
		}

	}

	static class Foo {

	}

	static class Bar {

	}

	@Configuration
	static class FooBarSpring {

		@Bean
		Supplier<Foo_Spring> fooSpring() {
			return () -> new Foo_Spring();
		}

		@Bean
		Bar_Spring barSpring() {
			return new Bar_Spring();
		}

	}

	static class Foo_Spring {

	}

	static class Bar_Spring {

	}

}
