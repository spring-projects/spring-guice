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

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 *
 */
public class EnableGuiceModulesTests {

	@AfterEach
	public void cleanUp() {
		System.clearProperty("spring.guice.dedup");
	}

	@Test
	public void test() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);
		assertThat(context.getBean(Foo.class)).isNotNull();
		context.close();
	}

	@Test
	public void testWithDedupFeatureEnabled() {
		System.setProperty("spring.guice.dedup", "true");
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);
		assertThat(context.getBean(Foo.class)).isNotNull();
		context.close();
	}

	@Test
	public void module() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ModuleConfig.class);
		assertThat(context.getBean(Foo.class)).isNotNull();
		context.close();
	}

	@Test
	public void moduleBean() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ModuleBeanConfig.class);
		assertThat(context.getBean(Foo.class)).isNotNull();
		context.close();
	}

	@Test
	public void testInjectorCreationDoesNotCauseCircularDependencyError() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MySpringConfig.class);
		assertThat(context.getBean(SpringProvidedBean.class)).isNotNull();
		context.close();
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

	@Configuration
	@EnableGuiceModules
	@GuiceModule(excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, value = Foo.class))
	protected static class TestConfig {

		@Autowired
		private Injector injector;

		@Bean
		public Foo foo() {
			return this.injector.getInstance(Foo.class);
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

	public static class SpringProvidedBean {

		public SpringProvidedBean(GuiceProvidedBean guiceProvidedBean) {
		}

	}

	public static class GuiceProvidedBean {

	}

	public static class GuiceService {

		@Inject
		public GuiceService(SpringProvidedBean springProvidedBean) {
		}

	}

	public static class MyGuiceModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(GuiceService.class).asEagerSingleton();
		}

		@Provides
		@Singleton
		public GuiceProvidedBean guiceProvidedBean() {
			return new GuiceProvidedBean();
		}

	}

	@Configuration
	@EnableGuiceModules
	public static class MySpringConfig {

		@Bean
		public SpringProvidedBean baz(GuiceProvidedBean guiceProvidedBean) {
			return new SpringProvidedBean(guiceProvidedBean);
		}

		@Bean
		public MyGuiceModule bazModule() {
			return new MyGuiceModule();
		}

	}

}
