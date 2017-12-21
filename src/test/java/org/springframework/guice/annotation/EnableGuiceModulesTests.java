/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.guice.annotation;

import javax.inject.Inject;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import static org.junit.Assert.assertNotNull;

/**
 * @author Dave Syer
 *
 */
public class EnableGuiceModulesTests {

	@Test
	public void test() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				TestConfig.class);
		assertNotNull(context.getBean(Foo.class));
		context.close();
	}

	@Test
	public void module() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ModuleConfig.class);
		assertNotNull(context.getBean(Foo.class));
		context.close();
	}

	@Test
	public void moduleBean() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ModuleBeanConfig.class);
		assertNotNull(context.getBean(Foo.class));
		context.close();
	}

	interface Service {
	}

	protected static class MyService implements Service {
	}

	public static class Foo {

		@Inject
		public Foo(Service service) {
			service.toString();
		}

	}

	@Configuration
	@EnableGuiceModules
	@GuiceModule(excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, value = Foo.class))
	protected static class TestConfig {

		@Autowired
		private Injector injector;

		@Bean
		public Foo foo() {
			return injector.getInstance(Foo.class);
		}

		@Bean
		public Service service() {
			return new MyService();
		}

	}

	@Configuration
	@EnableGuiceModules
	protected static class ModuleConfig extends AbstractModule {

		@Override
		protected void configure() {
			bind(Service.class).to(MyService.class);
		}

		@Bean
		public Foo service(Service service) {
			return new Foo(service);
		}

	}

	@Configuration
	@EnableGuiceModules
	protected static class ModuleBeanConfig {

		@Bean
		public MyModule module() {
			return new MyModule();
		}

		@Bean
		public Foo service(Service service) {
			return new Foo(service);
		}

	}

	protected static class MyModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(Service.class).to(MyService.class);
		}

	}

}
