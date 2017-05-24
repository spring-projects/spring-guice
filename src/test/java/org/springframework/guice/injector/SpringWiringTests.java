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

package org.springframework.guice.injector;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.AbstractCompleteWiringTests;

import com.google.inject.Injector;

/**
 * @author Dave Syer
 *
 */
public class SpringWiringTests extends AbstractCompleteWiringTests {

	@Override
	protected Injector createInjector() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(TestConfig.class);
		context.refresh();
		return new SpringInjector(context);
	}

	@Configuration
	public static class TestConfig {
		@Bean
		public Service service() {
			return new MyService();
		}
		@Bean
		public Thang thing() {
			return new Thang();
		}
		@Bean
		public Thang other() {
			return new Thang();
		}
        @Bean
        public Parameterized<String> parameterizedBean() {
            return new Parameterized<String>() {};
        }
	}

}
