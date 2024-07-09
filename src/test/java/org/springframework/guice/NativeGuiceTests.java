/*
 * Copyright 2016-2022 the original author or authors.
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
import com.google.inject.name.Names;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 *
 */
public class NativeGuiceTests {

	@Inject
	private Foo bar;

	@Test
	public void test() {
		Injector app = Guice.createInjector(new TestConfig());
		NativeGuiceTests instance = app.getInstance(NativeGuiceTests.class);
		assertThat(instance.bar).isNotNull();
	}

	public static class TestConfig extends AbstractModule {

		@Override
		protected void configure() {
			bind(Foo.class).annotatedWith(Names.named("bar")).to(Foo.class);
		}

	}

	public static class Foo {

	}

}
