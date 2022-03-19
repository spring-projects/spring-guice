/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.guice.module;

import javax.inject.Inject;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.util.Providers;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.AopTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringModuleGuiceBindingAwareTests {

	@Test
	public void testAllDependenciesInjectedAndLifeycleMethodsCalledOnce() {
		Injector injector = Guice.createInjector(new SimpleGuiceModule(),
				new SpringModule(BeanFactoryProvider.from(GuiceProjectWithSpringLibraryTestSpringConfig.class)));

		// check guice provided bindings
		assertThat(injector.getInstance(GuiceDependency1.class)).isNotNull();
		assertThat(injector.getInstance(IGuiceDependency1.class)).isNotNull();

		// check spring bindings as interface
		ISpringBean springBean = injector.getInstance(ISpringBean.class);
		assertThat(springBean).isNotNull();
		assertThat(springBean.getDep1()).isNotNull();
		assertThat(springBean.getDep2()).isNotNull();
		assertThat(springBean.getDep3()).isNotNull();

		// invoke a method to make sure we aren't dealing with a lazy proxy
		assertThat(springBean.getDep1().doWork()).isEqualTo("done");

		// check binding equality
		assertThat(injector.getInstance(IGuiceDependency1.class))
				.isSameAs(AopTestUtils.getTargetObject(springBean.getDep1()));
		assertThat(injector.getInstance(IGuiceDependency2.class))
				.isSameAs(AopTestUtils.getTargetObject(springBean.getDep2()));
		assertThat(injector.getInstance(IGuiceDependency3.class))
				.isSameAs(AopTestUtils.getTargetObject(springBean.getDep3()));
	}

	static class SimpleGuiceModule extends AbstractModule {

		@Override
		protected void configure() {
			// test normal binding
			bind(IGuiceDependency1.class).to(GuiceDependency1.class).in(Scopes.SINGLETON);
			// test instance binding
			bind(IGuiceDependency2.class).toInstance(new IGuiceDependency2() {
			});
			// test provider binding
			bind(IGuiceDependency3.class).toProvider(Providers.of(new IGuiceDependency3() {
			}));
		}

	}

	@Configuration
	static class GuiceProjectWithSpringLibraryTestSpringConfig {

		@Bean
		ISpringBean springDefinedSomething(IGuiceDependency1 dependency) {
			return new SpringBean(dependency);
		}

		@Bean
		ApplicationListener<ApplicationEvent> eventListener(final IGuiceDependency1 dependency) {
			return new ApplicationListener<ApplicationEvent>() {
				@Override
				public void onApplicationEvent(ApplicationEvent event) {
					dependency.doWork();
				}
			};
		}

	}

	interface IGuiceDependency1 {

		String doWork();

	}

	interface IGuiceDependency2 {

	}

	interface IGuiceDependency3 {

	}

	static class GuiceDependency1 implements IGuiceDependency1 {

		@Override
		public String doWork() {
			return "done";
		}

	}

	interface ISpringBean {

		IGuiceDependency1 getDep1();

		IGuiceDependency2 getDep2();

		IGuiceDependency3 getDep3();

	}

	static class SpringBean implements ISpringBean {

		private final IGuiceDependency1 dep1;

		@Inject
		private IGuiceDependency2 dep2;

		@Inject
		private IGuiceDependency3 dep3;

		@Inject
		SpringBean(IGuiceDependency1 dependency) {
			this.dep1 = dependency;
		}

		@Override
		public IGuiceDependency1 getDep1() {
			return this.dep1;
		}

		@Override
		public IGuiceDependency2 getDep2() {
			return this.dep2;
		}

		@Override
		public IGuiceDependency3 getDep3() {
			return this.dep3;
		}

	}

}
