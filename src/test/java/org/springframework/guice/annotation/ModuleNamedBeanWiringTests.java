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

package org.springframework.guice.annotation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.AbstractCompleteWiringTests;
import org.springframework.guice.injector.SpringInjector;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Dave Syer
 *
 */
public class ModuleNamedBeanWiringTests extends AbstractCompleteWiringTests {

	private AnnotationConfigApplicationContext context;

	@Override
	protected Injector createInjector() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfig.class);
		this.context.refresh();
		return new SpringInjector(this.context);
	}

	@Test
	public void bindToSpringBeanFromGuiceModule() throws Exception {
		assertNotNull(this.context.getBean(Spam.class));
	}

	@EnableGuiceModules
	@Configuration
	public static class TestConfig extends AbstractModule {

		@Autowired
		Service service;

		@Override
		protected void configure() {
			bind(Service.class).to(MyService.class);
		}

		@Bean
		public Spam spam(Service service) {
			return new Spam(service);
		}

		@Provides
		@Named("thing")
		public Thang thing() {
			return new Thang();
		}

		@Provides
		@Named("other")
		public Thang other() {
			return new Thang();
		}

		@Provides
		@Inject
		@Singleton
		public Baz baz(Service service) {
			return new Baz(service);
		}

		@Bean
		public Parameterized<String> parameterizedBean() {
			return new Parameterized<String>() {
			};
		}

	}

	protected static class Spam {

		public Spam(Service service) {
		}

	}

}
