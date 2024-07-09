/*
 * Copyright 2020-2022 the original author or authors.
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

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.guice.annotation.EnableGuiceModules;

import static org.assertj.core.api.Assertions.assertThat;

public class PrototypeScopedBeanTests {

	@Test
	public void testPrototypeScopedBeans() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ModulesConfig.class);
		Injector injector = context.getBean(Injector.class);
		GuiceService1 gs1 = injector.getInstance(GuiceService1.class);
		GuiceService2 gs2 = injector.getInstance((GuiceService2.class));
		assertThat(gs1).isNotNull();
		assertThat(gs2).isNotNull();
		assertThat(gs2.bean).isNotEqualTo(gs1.bean);
	}

	@Configuration
	@EnableGuiceModules
	static class ModulesConfig {

		@Bean
		static Module guiceModule() {
			return new AbstractModule() {
				@Override
				protected void configure() {
					bind(GuiceService1.class).asEagerSingleton();
					bind(GuiceService2.class).asEagerSingleton();
				}
			};
		}

		@Bean
		@Scope("prototype")
		PrototypeBean prototypeBean() {
			return new PrototypeBean();
		}

	}

	public static class PrototypeBean {

	}

	public static class GuiceService1 {

		@Inject
		PrototypeBean bean;

	}

	public static class GuiceService2 {

		@Inject
		PrototypeBean bean;

	}

}
