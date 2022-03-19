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

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scope;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

import static org.assertj.core.api.Assertions.assertThat;

public class ScopingTests {

	@Test
	public void verifyScopes() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ScopingTestsConfig.class);
		SomeSingletonDependency someSingletonDependency1 = context.getBean(SomeSingletonDependency.class);
		SomeSingletonDependency someSingletonDependency2 = context.getBean(SomeSingletonDependency.class);

		assertThat(someSingletonDependency1).isNotNull();
		assertThat(someSingletonDependency2).isNotNull();
		assertThat(someSingletonDependency2).isEqualTo(someSingletonDependency1);

		SomeNoScopeDependency someNoScopeDependency1 = context.getBean(SomeNoScopeDependency.class);
		SomeNoScopeDependency someNoScopeDependency2 = context.getBean(SomeNoScopeDependency.class);

		assertThat(someNoScopeDependency1).isNotNull();
		assertThat(someNoScopeDependency2).isNotNull();
		assertThat(someNoScopeDependency2).isNotEqualTo(someNoScopeDependency1);

		SomeCustomScopeDependency someCustomScopeDependency1 = context.getBean(SomeCustomScopeDependency.class);
		SomeCustomScopeDependency someCustomScopeDependency2 = context.getBean(SomeCustomScopeDependency.class);

		assertThat(someCustomScopeDependency1).isNotNull();
		assertThat(someCustomScopeDependency2).isNotNull();
		assertThat(someCustomScopeDependency2).isNotEqualTo(someCustomScopeDependency1);
		assertThat("custom").isEqualTo(someCustomScopeDependency1.value);
		assertThat("custom").isEqualTo(someCustomScopeDependency2.value);

		context.close();
	}

	public static class SomeSingletonDependency {

	}

	public static class SomeNoScopeDependency {

	}

	public static class SomeCustomScopeDependency {

		String value;

		public SomeCustomScopeDependency() {
		}

		public SomeCustomScopeDependency(String value) {
			this.value = value;
		}

	}

	public interface CustomScope extends Scope {

	}

	@EnableGuiceModules
	@Configuration
	static class ScopingTestsConfig {

		@Bean
		Module module() {
			return new AbstractModule() {
				@Override
				protected void configure() {
					CustomScope customScope = new CustomScope() {
						@SuppressWarnings("unchecked")
						@Override
						public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
							Provider<?> provider = () -> new SomeCustomScopeDependency("custom");
							return (Provider<T>) provider;
						}
					};
					bind(SomeSingletonDependency.class).asEagerSingleton();
					bind(SomeNoScopeDependency.class);
					bind(SomeCustomScopeDependency.class).in(customScope);
				}
			};
		}

	}

}
