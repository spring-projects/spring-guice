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

import java.util.Map;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;
import org.springframework.guice.injector.SpringInjector;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringAutowiredCollectionTests {

	@Test
	public void getAutowiredCollection() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(TestConfig.class);
		context.refresh();
		Injector injector = new SpringInjector(context);

		ServicesHolder servicesHolder = injector.getInstance(ServicesHolder.class);

		assertThat(servicesHolder.existingServices).hasSize(2);
		assertThat(servicesHolder.nonExistingServices).isEmpty();
	}

	@Configuration
	@EnableGuiceModules
	static class TestConfig {

		@Bean
		ServicesHolder serviceHolder(Map<String, Service> existingServices,
				Map<String, NonExistingService> nonExistingServices) {
			return new ServicesHolder(existingServices, nonExistingServices);
		}

		@Bean
		Service service() {
			return new Service();
		}

		@Bean
		static GuiceModule guiceServiceModule() {
			return new GuiceModule();
		}

	}

	static class Service {

	}

	static class NonExistingService {

	}

	static class GuiceModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(Service.class).asEagerSingleton();
		}

	}

	static class ServicesHolder {

		final Map<String, Service> existingServices;

		final Map<String, NonExistingService> nonExistingServices;

		ServicesHolder(Map<String, Service> existingServices, Map<String, NonExistingService> nonExistingServices) {
			this.existingServices = existingServices;
			this.nonExistingServices = nonExistingServices;
		}

	}

}
