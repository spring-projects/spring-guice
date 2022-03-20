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
import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.google.inject.name.Names;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PrivateModuleTests {

	private static AnnotationConfigApplicationContext context;

	@BeforeAll
	public static void init() {
		context = new AnnotationConfigApplicationContext(PrivateModuleTestConfig.class);
	}

	@AfterAll
	public static void cleanup() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void verifyPrivateModulesCanExposeBindings() {
		Injector injector = context.getBean(Injector.class);
		SomeInterface injectorProvidedPrivateBinding = injector
				.getInstance(Key.get(SomeInterface.class, Names.named("exposed")));
		assertThat(injectorProvidedPrivateBinding).isNotNull();
		SomeInterface springProvidedPrivateBinding = context.getBean(SomeInterface.class);
		assertThat(springProvidedPrivateBinding).isNotNull();
		SomeInterface namedPrivateBinding = BeanFactoryAnnotationUtils.qualifiedBeanOfType(context.getBeanFactory(),
				SomeInterface.class, "exposed");
		assertThat(namedPrivateBinding).isNotNull();
		assertThat(springProvidedPrivateBinding).isEqualTo(injectorProvidedPrivateBinding);
		assertThat(namedPrivateBinding).isEqualTo(injectorProvidedPrivateBinding);
		String beanDependingOnPrivateBinding = context.getBean("somethingThatWantsAPrivateBinding", String.class);
		assertThat(beanDependingOnPrivateBinding).isNotNull();
		assertThat(beanDependingOnPrivateBinding).isEqualTo("foo");
	}

	@Test
	public void verifyPrivateModulesPrivateBindingsAreNotExposedViaInjector() {
		Injector injector = context.getBean(Injector.class);
		assertThatExceptionOfType(ConfigurationException.class)
				.isThrownBy(() -> injector.getInstance(Key.get(SomeInterface.class, Names.named("notexposed"))));
	}

	@Test
	public void verifyPrivateModulesPrivateBindingsAreNotExposedViaSpring() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> context.getBean("notexposed", SomeInterface.class));
	}

	@Test
	public void verifyPrivateModulesPrivateBindingsAreNotExposedViaSpringWithQualifier() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> BeanFactoryAnnotationUtils
				.qualifiedBeanOfType(context.getBeanFactory(), SomeInterface.class, "notexposed"));
	}

	public interface SomeInterface {

	}

	public static class SomePrivateBinding implements SomeInterface {

	}

	public static class SomePrivateModule extends PrivateModule {

		@Override
		protected void configure() {
			bind(SomeInterface.class).annotatedWith(Names.named("exposed")).to(SomePrivateBinding.class)
					.asEagerSingleton();
			bind(SomeInterface.class).annotatedWith(Names.named("notexposed")).to(SomePrivateBinding.class)
					.asEagerSingleton();
			expose(SomeInterface.class).annotatedWith(Names.named("exposed"));
		}

	}

	@EnableGuiceModules
	@Configuration
	static class PrivateModuleTestConfig {

		@Bean
		String somethingThatWantsAPrivateBinding(SomeInterface privateBinding) {
			return "foo";
		}

		@Bean
		static Module module() {
			return new AbstractModule() {
				@Override
				protected void configure() {
					install(new SomePrivateModule());
				}
			};
		}

	}

}
