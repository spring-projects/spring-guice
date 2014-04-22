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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.guice.annotation.GuiceModule;
import org.springframework.guice.module.SpringModule;

import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * @author Dave Syer
 *
 */
public class GuiceModuleAnnotationTests {

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Test
	public void includes() throws Exception {
		Injector injector = createInjector(TestConfig.class, MetadataIncludesConfig.class);
		assertNotNull(injector.getBinding(Service.class));
	}

	@Test
	public void includesNames() throws Exception {
		Injector injector = createInjector(TestConfig.class, MetadataIncludeNamesConfig.class);
		assertNotNull(injector.getBinding(Service.class));
	}

	@Test
	public void includesPatterns() throws Exception {
		Injector injector = createInjector(TestConfig.class, MetadataIncludePatternsConfig.class);
		assertNotNull(injector.getBinding(Service.class));
	}

	@Test
	public void excludes() throws Exception {
		Injector injector = createInjector(TestConfig.class, MetadataExcludesConfig.class);
		expected.expect(ConfigurationException.class);
		assertNull(injector.getInstance(Service.class));
	}

	@Test
	public void twoIncludes() throws Exception {
		Injector injector = createInjector(TestConfig.class, MetadataIncludesConfig.class, MetadataMoreIncludesConfig.class);
		assertNotNull(injector.getBinding(Service.class));
	}

	private Injector createInjector(Class<?>... config) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(config);
		context.refresh();
		Injector injector = Guice.createInjector(new SpringModule(context));
		return injector;
	}

	interface Service {
	}

	protected static class MyService implements Service {
	}

	public static class Foo {

		@Autowired
		public Foo(Service service) {
		}

	}

	@Configuration
	@GuiceModule(excludeFilters=@Filter(type=FilterType.REGEX, pattern=".*"))
	protected static class MetadataExcludesConfig {
	}

	@Configuration
	@GuiceModule(excludePatterns=".*")
	protected static class MetadataExcludesNameConfig {
	}

	@Configuration
	@GuiceModule(includeFilters=@Filter(type=FilterType.ASSIGNABLE_TYPE, value=Service.class))
	protected static class MetadataIncludesConfig {
	}
	
	@Configuration
	@GuiceModule(includeNames="*service") // Bean name filter
	protected static class MetadataIncludeNamesConfig {
	}
	
	@Configuration
	@GuiceModule(includePatterns=".*service") // Bean name filter
	protected static class MetadataIncludePatternsConfig {
	}
	
	@Configuration
	@GuiceModule(includeFilters=@Filter(type=FilterType.ASSIGNABLE_TYPE, value=Foo.class))
	protected static class MetadataMoreIncludesConfig {
	}
	
	@Configuration
	public static class TestConfig {
		@Bean
		public Service service() {
			return new MyService();
		}
	}
}
