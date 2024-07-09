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

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

import static org.assertj.core.api.Assertions.assertThat;

public class DuplicateNamesDifferentTypesTests {

	@Test
	public void verifyNoDuplicateBindingErrorWhenDedupeEnabled() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				DuplicateNamesDifferentTypesTestsConfig.class);

		// Check Guice @Named
		assertThat(context.getBean(SomeNamedDepWithType1.class)).isNotNull();
		assertThat(context.getBean(SomeNamedDepWithType2.class)).isNotNull();
		assertThat(BeanFactoryAnnotationUtils.qualifiedBeanOfType(context.getBeanFactory(), SomeNamedDepWithType1.class,
				"sameNameDifferentType")).isNotNull();

		// Check jakarta @Named
		assertThat(context.getBean(SomeJakartaNamedDepWithType1.class)).isNotNull();
		assertThat(context.getBean(SomeJakartaNamedDepWithType2.class)).isNotNull();
		assertThat(BeanFactoryAnnotationUtils.qualifiedBeanOfType(context.getBeanFactory(),
				SomeJakartaNamedDepWithType1.class, "sameJakartaName")).isNotNull();
		context.getBeansOfType(SomeJakartaNamedDepWithType1.class);

		context.close();
	}

	public static class SomeNamedDepWithType1 {

	}

	public static class SomeNamedDepWithType2 {

	}

	public static class SomeJakartaNamedDepWithType1 {

	}

	public static class SomeJakartaNamedDepWithType2 {

	}

	public static class SomeClassWithDeps {

		@Autowired
		@Qualifier("sameJakartaName2")
		SomeJakartaNamedDepWithType1 qualified;

		@Autowired
		@Named("sameJakartaName2")
		SomeJakartaNamedDepWithType1 named;

		@Autowired
		@jakarta.inject.Named("sameJakartaName2")
		SomeJakartaNamedDepWithType1 jakartaNamed;

	}

	@EnableGuiceModules
	@Configuration
	static class DuplicateNamesDifferentTypesTestsConfig {

		@Bean
		static Module module() {
			return new AbstractModule() {
				@Override
				protected void configure() {
					bind(SomeNamedDepWithType1.class).annotatedWith(Names.named("sameNameDifferentType"))
							.to(SomeNamedDepWithType1.class);
					bind(SomeNamedDepWithType2.class).annotatedWith(Names.named("sameNameDifferentType"))
							.to(SomeNamedDepWithType2.class);
				}

				@Provides
				@Named("sameJakartaName")
				SomeJakartaNamedDepWithType1 someJakartaNamedDepWithType1() {
					return new SomeJakartaNamedDepWithType1();
				}

				@Provides
				@Named("sameJakartaName")
				SomeJakartaNamedDepWithType2 someJakartaNamedDepWithType2() {
					return new SomeJakartaNamedDepWithType2();
				}
			};
		}

	}

}
