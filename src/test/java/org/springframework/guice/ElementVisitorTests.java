/*
 * Copyright 2018-2022 the original author or authors.
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

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;
import org.springframework.guice.annotation.InjectorFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class ElementVisitorTests {

	private static AnnotationConfigApplicationContext context;

	@BeforeAll
	public static void init() {
		System.setProperty("spring.guice.dedup", "true");
		context = new AnnotationConfigApplicationContext(ElementVisitorTestConfig.class);
	}

	@AfterAll
	public static void cleanup() {
		System.clearProperty("spring.guice.dedup");
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void verifySpringModuleDoesNotBreakWhenUsingElementVisitors() {
		ElementVisitorTestSpringBean testSpringBean = context.getBean(ElementVisitorTestSpringBean.class);
		assertThat(testSpringBean.toString()).isEqualTo("spring created");
		ElementVisitorTestGuiceBean testGuiceBean = context.getBean(ElementVisitorTestGuiceBean.class);
		assertThat(testGuiceBean.toString()).isEqualTo("spring created");
	}

	public static class ElementVisitorTestSpringBean {

		@Override
		public String toString() {
			return "default";
		}

	}

	public static class ElementVisitorTestGuiceBean {

		@Inject
		ElementVisitorTestSpringBean springBean;

		@Override
		public String toString() {
			return this.springBean.toString();
		}

	}

	public static class DuplicateBean {

	}

	@EnableGuiceModules
	@Configuration
	static class ElementVisitorTestConfig {

		@Bean
		ElementVisitorTestSpringBean testBean() {
			return new ElementVisitorTestSpringBean() {
				@Override
				public String toString() {
					return "spring created";
				}
			};
		}

		@Bean
		static Module module() {
			return new AbstractModule() {
				@Override
				protected void configure() {
					binder().requireExplicitBindings();
					bind(ElementVisitorTestGuiceBean.class).asEagerSingleton();
				}
			};
		}

		@Bean
		InjectorFactory injectorFactory() {
			return new InjectorFactory() {
				@Override
				public Injector createInjector(List<Module> modules) {
					List<Element> elements = Elements.getElements(Stage.TOOL, modules);
					return Guice.createInjector(Stage.PRODUCTION, Elements.getModule(elements));
				}
			};
		}

		@Bean
		DuplicateBean dupeBean1() {
			return new DuplicateBean();
		}

		@Bean
		DuplicateBean dupeBean2() {
			return new DuplicateBean();
		}

	}

}
