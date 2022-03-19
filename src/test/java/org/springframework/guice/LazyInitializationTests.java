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
import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.guice.annotation.EnableGuiceModules;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LazyInitializationTests {

	@Test
	public void lazyAnnotationIsRespectedOnInjectionPointForGuiceBinding() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(TestConfig.class);
		context.register(GuiceConfig.class);
		context.refresh();

		Service service = context.getBean(Service.class);

		assertTrue(AopUtils.isAopProxy(service.getBean()));
		assertNotNull(context.getBean(TestBean.class));
	}

	@Test
	public void lazyAnnotationIsRespectedOnInjectionPointForSpringBinding() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(TestConfig.class);
		context.register(SpringConfig.class);
		context.refresh();

		Service service = context.getBean(Service.class);

		assertTrue(AopUtils.isAopProxy(service.getBean()));
		assertNotNull(context.getBean(TestBean.class));
	}

	@Configuration
	@EnableGuiceModules
	static class TestConfig {

		@Bean
		Service service(@Lazy TestBean bean) {
			return new Service(bean);
		}

	}

	@Configuration
	static class GuiceConfig {

		@Bean
		GuiceModule guiceModule() {
			return new GuiceModule();
		}

	}

	@Configuration
	static class SpringConfig {

		@Bean
		TestBean testBean() {
			return new TestBean();
		}

	}

	static class GuiceModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(TestBean.class).asEagerSingleton();
		}

	}

	static class Service {

		private final TestBean bean;

		Service(TestBean bean) {
			this.bean = bean;
		}

		TestBean getBean() {
			return this.bean;
		}

	}

	static class TestBean {

	}

}
