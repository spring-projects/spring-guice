/*
 * Copyright 2021-2022 the original author or authors.
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

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;
import org.springframework.guice.annotation.InjectorFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DevelepmentStageInjectorTest {

	@BeforeClass
	public static void init() {
		System.setProperty("spring.guice.stage", "DEVELOPMENT");
	}

	@AfterClass
	public static void cleanup() {
		System.clearProperty("spring.guice.stage");
	}

	@Test
	public void testLazyInitBean() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				DevelepmentStageInjectorTest.ModulesConfig.class);
		TestGuiceModule testGuiceModule = context.getBean(TestGuiceModule.class);
		assertFalse(testGuiceModule.getProviderExecuted());
		GuiceToken guiceToken = context.getBean(GuiceToken.class);
		assertTrue(testGuiceModule.getProviderExecuted());
		context.close();
	}

	@Configuration
	@EnableGuiceModules
	static class ModulesConfig {

		@Bean
		TestGuiceModule testGuiceModule() {
			return new TestGuiceModule();
		}

		@Bean
		InjectorFactory injectorFactory() {
			return new TestDevelopmentStageInjectorFactory();
		}

	}

	static class TestGuiceModule extends AbstractModule {

		private boolean providerExecuted = false;

		boolean getProviderExecuted() {
			return this.providerExecuted;
		}

		@Override
		protected void configure() {
		}

		@Provides
		@Singleton
		GuiceToken guiceToken() {
			this.providerExecuted = true;
			return new GuiceToken();
		}

	}

	static class TestDevelopmentStageInjectorFactory implements InjectorFactory {

		@Override
		public Injector createInjector(List<Module> modules) {
			return Guice.createInjector(Stage.DEVELOPMENT, modules);
		}

	}

	static class GuiceToken {

	}

}
