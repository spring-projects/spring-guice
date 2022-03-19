/*
 * Copyright 2016-2022 the original author or authors.
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

import com.google.inject.Guice;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;
import org.springframework.guice.annotation.InjectorFactory;

public class InjectorFactoryTests {

	private static final InjectorFactory injectorFactory = Mockito.mock(InjectorFactory.class);

	@Before
	public void init() {
		Mockito.when(injectorFactory.createInjector(Mockito.anyList())).thenReturn(Guice.createInjector());
	}

	@Test
	public void testCustomInjectorIsCreated() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(InjectorFactoryConfig.class,
				ModulesConfig.class);
		Mockito.verify(injectorFactory, Mockito.times(1)).createInjector(Mockito.anyList());
		context.close();
	}

	@Test(expected = ApplicationContextException.class)
	public void testMultipleInjectorFactoriesThrowsApplicationContextException() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(InjectorFactoryConfig.class,
				SecondInjectorFactoryConfig.class, ModulesConfig.class);
		context.close();
	}

	@Configuration
	@EnableGuiceModules
	static class ModulesConfig {

	}

	@Configuration
	static class InjectorFactoryConfig {

		@Bean
		InjectorFactory injectorFactory() {
			return injectorFactory;
		}

	}

	@Configuration
	static class SecondInjectorFactoryConfig {

		@Bean
		InjectorFactory injectorFactory2() {
			return injectorFactory;
		}

	}

}
