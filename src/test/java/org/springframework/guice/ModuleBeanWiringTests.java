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

package org.springframework.guice;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;

/**
 * @author Dave Syer
 *
 */
public class ModuleBeanWiringTests extends AbstractCompleteWiringTests {

	private AnnotationConfigApplicationContext context;

	@Override
	protected Injector createInjector() {
		context = new AnnotationConfigApplicationContext();
		context.register(TestConfig.class);
		context.refresh();
		return new SpringInjector(context);
	}

	@Test
	public void bindToSpringBeanFromGuiceModule() throws Exception {
		assertNotNull(context.getBean(Spam.class));
	}

	@EnableGuiceModules
	@Configuration
	public static class TestConfig extends AbstractModule {
		@Override
		protected void configure() {
			bind(Service.class).to(MyService.class);
		}

		@Bean
		public Spam spam(Service service) {
			return new Spam(service);
		}
	}

	protected static class Spam {
		public Spam(Service service) {
		}
	}

}
