/*
 * Copyright 2015 the original author or authors.
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
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

import static org.assertj.core.api.Assertions.assertThat;

public class SetBindingTests {

	@Inject
	private Benz bar;

	@Test
	public void testNativeGuiceBinding() {
		Injector app = Guice.createInjector(new AutomobileModule());
		SetBindingTests instance = app.getInstance(SetBindingTests.class);
		assertThat(instance.bar).isNotNull();
	}

	@Test
	void testSpringBinding() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(SpringGuiceConfig.class);
		context.refresh();
		Benz car = context.getBean(SpringGuiceConfig.class).getBenz();
		assertThat(car).isNotNull();
		assertThat(car.getModel()).contains("220");
		context.close();
	}

	@Configuration
	@EnableGuiceModules
	public static class SpringGuiceConfig {

		@Autowired
		private Benz benz;

		public Benz getBenz() {
			return this.benz;
		}

		@Bean
		public static Module module() {
			return new AutomobileModule();
		}

	}

	interface Car {

		String getModel();

	}

	static class Audi implements Car {

		@Override
		public String getModel() {
			return "Audi A4";
		}

	}

	static class Benz implements Car {

		@Override
		public String getModel() {
			return "C 220";
		}

	}

	static class AutomobileModule extends AbstractModule {

		@Override
		protected void configure() {
			Multibinder<Car> uriBinder = Multibinder.newSetBinder(binder(), Car.class);
			uriBinder.addBinding().to(Audi.class);
			uriBinder.addBinding().to(Benz.class);
		}

	}

}
