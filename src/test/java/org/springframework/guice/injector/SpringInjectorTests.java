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
package org.springframework.guice.injector;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.AbstractCompleteWiringTests.Baz;
import org.springframework.guice.AbstractCompleteWiringTests.MyService;
import org.springframework.guice.AbstractCompleteWiringTests.Service;

import com.google.inject.Key;
import com.google.inject.name.Names;

public class SpringInjectorTests {
	
	@Rule
	public ExpectedException expected = ExpectedException.none();

	private SpringInjector injector = new SpringInjector(create());

	private AnnotationConfigApplicationContext context;
	
	@After
	public void close() {
		if (context!=null) {
			context.close();
		}
	}

	@Test
	public void instance() {
		assertNotNull(injector.getInstance(Service.class));
	}

	@Test
	public void multiple() {
		injector = new SpringInjector(create(Additional.class));
		expected.expect(NoUniqueBeanDefinitionException.class);
		assertNotNull(injector.getInstance(Service.class));
	}

	@Test
	public void named() {
		injector = new SpringInjector(create(Additional.class));
		assertNotNull(injector.getInstance(Key.get(Service.class, Names.named("service"))));
	}

	@Test
	public void provider() {
		assertNotNull(injector.getProvider(Service.class).get());
	}

	@Test
	public void bindNewObject() {
		assertNotNull(injector.getInstance(Baz.class));
	}

	private ApplicationContext create(Class<?>... config) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(TestConfig.class);
		if (config.length > 0) {
			context.register(config);
		}
		context.refresh();
		this.context = context;
		return context;
	}

	@Configuration
	public static class Additional {
		@Bean
		public Service another() {
			return new MyService();
		}
	}
	@Configuration
	public static class TestConfig {
		@Bean
		public Service service() {
			return new MyService();
		}
	}
}
