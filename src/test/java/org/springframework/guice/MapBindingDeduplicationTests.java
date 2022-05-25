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

import java.util.Map;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.multibindings.MapBinder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.guice.annotation.EnableGuiceModules;
import org.springframework.guice.module.SpringModule;

import static org.assertj.core.api.Assertions.assertThat;

public class MapBindingDeduplicationTests {

	@AfterAll
	public static void cleanUp() {
		System.clearProperty("spring.guice.dedup");
	}

	@Test
	public void mapBindingGuiceOnly() {
		System.setProperty("spring.guice.dedup", "false");
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				MapBindingGuiceOnlyTestsConfig.class);

		String[] beanNamesForType = context
				.getBeanNamesForType(ResolvableType.forClassWithGenerics(Map.class, String.class, Provider.class));
		@SuppressWarnings("unchecked")
		Map<String, Provider<Dependency>> dependencyProvider = (Map<String, Provider<Dependency>>) context
				.getBean(beanNamesForType[0]);

		assertThat(dependencyProvider.size()).isEqualTo(2);
		assertThat(dependencyProvider.get("someQualifier").get()).isInstanceOf(SomeDependency.class);

		context.close();
	}

	@Test
	public void mapBindingConflictingConcreteClass() {
		System.setProperty("spring.guice.dedup", "true");
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				MapBindingConcreteClassTestsConfig.class);

		String[] beanNamesForType = context
				.getBeanNamesForType(ResolvableType.forClassWithGenerics(Map.class, String.class, Provider.class));
		@SuppressWarnings("unchecked")
		Map<String, Provider<Dependency>> dependencyProvider = (Map<String, Provider<Dependency>>) context
				.getBean(beanNamesForType[0]);

		assertThat(dependencyProvider.size()).isEqualTo(2);
		assertThat(dependencyProvider.get("someQualifier").get()).isInstanceOf(SomeDependency.class);

		SomeDependency someDependency = context.getBean(SomeDependency.class);
		assertThat(someDependency.getSource()).isEqualTo(SpringModule.SPRING_GUICE_SOURCE);

		context.close();
	}

	interface Dependency {

	}

	public static class SomeDependency implements Dependency {

		private String source = "guice";

		public void setSource(String source) {
			this.source = source;
		}

		public String getSource() {
			return this.source;
		}

	}

	public static class SomeOptionalDependency implements Dependency {

	}

	@EnableGuiceModules
	@Configuration
	static class MapBindingGuiceOnlyTestsConfig {

		@Bean
		static Module module() {
			return new AbstractModule() {
				@Override
				protected void configure() {
					MapBinder<String, Dependency> bindings = MapBinder.newMapBinder(binder(), String.class,
							Dependency.class);
					bindings.addBinding("someQualifier").to(SomeDependency.class);
					bindings.addBinding("someOtherQualifier").to(SomeOptionalDependency.class);
				}
			};
		}

	}

	@EnableGuiceModules
	@Configuration
	static class MapBindingConcreteClassTestsConfig {

		@Bean
		SomeDependency dependency() {
			SomeDependency someDependency = new SomeDependency();
			someDependency.setSource(SpringModule.SPRING_GUICE_SOURCE);
			return someDependency;
		}

		@Bean
		static Module module() {
			return new AbstractModule() {
				@Override
				protected void configure() {
					MapBinder<String, Dependency> bindings = MapBinder.newMapBinder(binder(), String.class,
							Dependency.class);
					bindings.addBinding("someQualifier").to(SomeDependency.class);
					// Intentionally duplicate the binding to ensure that every key is
					// available after deduplication
					bindings.addBinding("someQualifier").to(SomeDependency.class);
					bindings.addBinding("someOtherQualifier").to(SomeOptionalDependency.class);
				}
			};
		}

	}

}
