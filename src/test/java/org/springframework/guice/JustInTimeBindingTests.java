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

import javax.inject.Inject;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

import static org.junit.Assert.assertNotNull;

public class JustInTimeBindingTests {

	@After
	public void tearDown() {
		System.clearProperty("spring.guice.autowireJIT");
	}

	@Test
	public void springWithJustInTimeBinding() {
		System.setProperty("spring.guice.autowireJIT", "true");
		assertNotNull(springGetFoo());
	}

	@Test(expected = UnsatisfiedDependencyException.class)
	public void springWithoutJustInTimeBinding() {
		System.setProperty("spring.guice.autowireJIT", "false");
		springGetFoo();
	}

	@SuppressWarnings("resource")
	private Foo springGetFoo() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ModulesConfig.class);
		context.getDefaultListableBeanFactory().registerBeanDefinition(Foo.class.getSimpleName(),
				new RootBeanDefinition(Foo.class));
		return context.getBean(Foo.class);
	}

	@Configuration
	@EnableGuiceModules
	static class ModulesConfig {

	}

	public static class Service {

	}

	public static class Foo {

		Service service;

		@Inject
		public Foo(Service service) {
			this.service = service;
		}

	}

}
