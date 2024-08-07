/*
 * Copyright 2014-2022 the original author or authors.
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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringModuleWrappedTests {

	@Test
	public void testDependenciesFromWrappedModule() {
		Injector injector = Guice.createInjector(
				new SpringModule(BeanFactoryProvider.from(TestConfig.class, ModuleProviderConfig.class)));
		assertThat(injector.getInstance(Baz.class)).isNotNull();
	}

	@Configuration
	public static class TestConfig {

		@Bean
		public Baz baz(Service service) {
			return new Baz(service);
		}

	}

	interface Service {

	}

	protected static class MyService implements Service {

	}

	public static class Foo {

		@Inject
		public Foo(@Named("service") Service service) {
			service.toString();
		}

	}

	public static class Baz {

		@Inject
		public Baz(Service service) {
		}

	}

	@Configuration
	@EnableGuiceModules
	protected static class ModuleProviderConfig {

		@Bean
		public static ProviderModule module() {
			return new ProviderModule();
		}

		@Bean
		public Foo service(Service service) {
			return new Foo(service);
		}

	}

	protected static class ProviderModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(Service.class).toProvider(() -> new MyService());
		}

	}

}
