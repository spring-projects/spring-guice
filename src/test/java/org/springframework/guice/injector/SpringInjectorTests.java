/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.guice.injector;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.AbstractCompleteWiringTests.Baz;
import org.springframework.guice.AbstractCompleteWiringTests.MyService;
import org.springframework.guice.AbstractCompleteWiringTests.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SpringInjectorTests {

	private SpringInjector injector = new SpringInjector(create());

	private AnnotationConfigApplicationContext context;

	@AfterEach
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void instance() {
		assertThat(this.injector.getInstance(Service.class)).isNotNull();
	}

	@Test
	public void multiple() {
		this.injector = new SpringInjector(create(Additional.class));
		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class)
				.isThrownBy(() -> this.injector.getInstance(Service.class));
	}

	@Test
	public void named() {
		this.injector = new SpringInjector(create(Additional.class));
		assertThat(this.injector.getInstance(Key.get(Service.class, Names.named("service")))).isNotNull();
	}

	@Test
	public void provider() {
		assertThat(this.injector.getProvider(Service.class).get()).isNotNull();
	}

	@Test
	public void bindNewObject() {
		assertThat(this.injector.getInstance(Baz.class)).isNotNull();
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
