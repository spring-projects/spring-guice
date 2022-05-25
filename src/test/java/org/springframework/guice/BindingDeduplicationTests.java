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

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Names;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class BindingDeduplicationTests {

	@BeforeEach
	public void setup() {
		System.setProperty("spring.guice.dedup", "true");
	}

	@AfterEach
	public void cleanUp() {
		System.clearProperty("spring.guice.dedup");
	}

	@Test
	public void verifyNoDuplicateBindingErrorWhenDedupeEnabled() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BindingDeduplicationTestsConfig.class)) {
			Dependency dependency = context.getBean(Dependency.class);
			assertThat(dependency).isNotNull();

			OptionalDependency optionalDependency = context.getBean(OptionalDependency.class);
			assertThat(optionalDependency).isNotNull();
		}
	}

	@Test
	public void annotatedBindingDoesNotDuplicate() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BindingDeduplicationTestsConfig.class)) {
			FirstInterface firstInterface = context.getBean(FirstInterface.class);
			assertThat(firstInterface).isNotNull();
		}
	}

	@Test
	public void untargettedBindingDoesNotDuplicate() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BindingDeduplicationTestsConfig.class)) {
			UntargettedDependency untargettedDependency = context.getBean(UntargettedDependency.class);
			assertThat(untargettedDependency).isNotNull();
		}
	}

	@Test
	public void setBindingDoesNotDuplicate() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BindingDeduplicationTestsConfig.class)) {
			SetProvided setProvided = context.getBean(SetProvided.class);
			assertThat(setProvided).isNotNull();
		}
	}

	@Test
	public void springBindingIsDuplicated() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BindingDeduplicationTestsConfig.class);

		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class)
				.isThrownBy(() -> context.getBean(String.class));

		context.close();
	}

	@Test
	public void verifyDuplicateBindingErrorWhenDedupeNotEnabled() {
		System.setProperty("spring.guice.dedup", "false");
		assertThatExceptionOfType(CreationException.class).isThrownBy(() -> {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
					BindingDeduplicationTestsConfig.class);
			context.close();
		});
	}

	public interface Dependency {

	}

	private static class PrivateDependency implements Dependency {

	}

	public static class SomeSingleton {

	}

	public interface OptionalDependency {

	}

	public static class SomeOptionalDependency implements OptionalDependency {

	}

	interface FirstInterface {

	}

	interface SecondInterface {

	}

	static class MultiInterfaceSingleton implements FirstInterface, SecondInterface {

	}

	static class UntargettedDependency {

	}

	interface SetProvided {

	}

	public static class SomeSetProvided implements SetProvided {

	}

	@EnableGuiceModules
	@Configuration
	static class BindingDeduplicationTestsConfig {

		@Bean
		SomeSingleton someSingleton() {
			return new SomeSingleton();
		}

		@Bean
		PrivateDependency privateDependency() {
			return new PrivateDependency();
		}

		@Bean
		OptionalDependency someOptionalDependency() {
			return new SomeOptionalDependency();
		}

		@Bean
		String barString() {
			return "bar";
		}

		@Bean
		SomeSetProvided someSetProvided() {
			return new SomeSetProvided();
		}

		@Bean
		static Module module() {
			return new AbstractModule() {
				@Override
				protected void configure() {
					bind(Dependency.class).to(PrivateDependency.class);
					bind(SomeSingleton.class).asEagerSingleton();

					OptionalBinder.newOptionalBinder(binder(), OptionalDependency.class).setDefault()
							.to(SomeOptionalDependency.class);

					Multibinder<SetProvided> setBinder = Multibinder.newSetBinder(binder(), SetProvided.class);
					setBinder.addBinding().toInstance(new SomeSetProvided());

					bind(UntargettedDependency.class);

					// Untargetted binding to provide a singleton for the interface
					// bindings
					bind(MultiInterfaceSingleton.class).in(Scopes.SINGLETON);
					bind(FirstInterface.class).to(MultiInterfaceSingleton.class).in(Scopes.SINGLETON);
					bind(SecondInterface.class).to(MultiInterfaceSingleton.class).in(Scopes.SINGLETON);

					bind(String.class).annotatedWith(Names.named("fooString")).toInstance("foo");
				}
			};
		}

	}

}
