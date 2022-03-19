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
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test injecting Map
 *
 * @author Dave Syer
 */
public class MapWiringTests {

	// Test Guice -> Spring direction
	@SuppressWarnings({ "resource", "unused" })
	@Test
	public void testProvidesMap() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ModulesConfig.class,
				FooBar.class);
		Bar bar = context.getBean(Bar.class);
	}

	@Configuration
	@EnableGuiceModules
	static class ModulesConfig {

		@Bean
		TestConfig testConfig() {
			return new TestConfig();
		}

	}

	@Configuration
	static class FooBar {

		@Bean
		Bar foo(Map<String, Foo> foos) {
			assertThat(foos.isEmpty()).isFalse();
			return new Bar();
		}

	}

	static class TestConfig extends AbstractModule {

		@Override
		protected void configure() {
			bind(Foo.class);
		}

	}

	static class Foo {

	}

	static class Bar {

	}

}
