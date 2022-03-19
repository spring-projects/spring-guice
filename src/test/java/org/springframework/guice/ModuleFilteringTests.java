/*
 * Copyright 2019-2022 the original author or authors.
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
import com.google.inject.Module;
import com.google.inject.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;
import org.springframework.guice.annotation.InjectorFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ModuleFilteringTests {

	@AfterAll
	public static void cleanUp() {
		System.clearProperty("spring.guice.modules.exclude");
	}

	@Test
	public void verifyAllIsWellWhenNoModulesFiltered() {
		System.setProperty("spring.guice.modules.exclude", "FilterSomeNonExistentModule");
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
					ModuleFilteringTestsConfig.class);
			SomeInterface someDependency = context.getBean(SomeInterface.class);
			assertThat(someDependency).isNotNull();
			context.close();
		});
	}

	@Test
	public void verifyFilteredModuleIsFiltered() {
		System.setProperty("spring.guice.modules.exclude", "FilterThisModule");
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ModuleFilteringTestsConfig.class)) {
			assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
					.isThrownBy(() -> context.getBean(SomeInterface.class));
		}
	}

	public interface SomeInterface {

	}

	public static class SomeDependency implements SomeInterface {

		public SomeDependency() {
			throw new RuntimeException("Should never be instantiated");
		}

	}

	public static class FilterThisModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(SomeInterface.class).to(SomeDependency.class).asEagerSingleton();
		}

	}

	@EnableGuiceModules
	@Configuration
	static class ModuleFilteringTestsConfig {

		@Bean
		InjectorFactory injectorFactory() {
			return (modules) -> Guice.createInjector(Stage.PRODUCTION, modules);
		}

		@Bean
		Module module() {
			return new AbstractModule() {

				@Override
				protected void configure() {
					install(new FilterThisModule());
				}
			};
		}

	}

}
