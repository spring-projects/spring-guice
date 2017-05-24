package org.springframework.guice.module;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import javax.inject.Inject;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;
import org.springframework.guice.injector.SpringInjector;
import org.springframework.test.util.AopTestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.util.Providers;

public class SpringModuleGuiceBindingAwareTests {

	@Test
	public void testAllDependenciesInjectedAndLifeycleMethodsCalledOnce() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(GuiceProjectWithSpringLibraryTestSpringConfig.class, SimpleGuiceModule.class);
		context.refresh();
		Injector injector = new SpringInjector(context);

		// check guice provided bindings
		assertNotNull(injector.getInstance(GuiceDependency1.class));
		assertNotNull(injector.getInstance(IGuiceDependency1.class));

		// check spring bindings as interface
		ISpringBean springBean = injector.getInstance(ISpringBean.class);
		assertNotNull(springBean);
		assertNotNull(springBean.getDep1());
		assertNotNull(springBean.getDep2());
		assertNotNull(springBean.getDep3());

		// check binding equality
		assertSame(injector.getInstance(IGuiceDependency1.class), AopTestUtils.getTargetObject(springBean.getDep1()));
		assertSame(injector.getInstance(IGuiceDependency2.class), AopTestUtils.getTargetObject(springBean.getDep2()));
		assertSame(injector.getInstance(IGuiceDependency3.class), AopTestUtils.getTargetObject(springBean.getDep3()));
		
		context.close();
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
	@EnableGuiceModules
	static class GuiceProjectWithSpringLibraryTestSpringConfig {

		@Bean
		public ISpringBean springDefinedSomething(IGuiceDependency1 dependency) {
			return new SpringBean(dependency);
		}

	}

	static interface IGuiceDependency1 {
	}

	static interface IGuiceDependency2 {
	}

	static interface IGuiceDependency3 {
	}

	static class GuiceDependency1 implements IGuiceDependency1 {
	}

	static interface ISpringBean {
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
		public SpringBean(IGuiceDependency1 dependency) {
			this.dep1 = dependency;
		}

		@Override
		public IGuiceDependency1 getDep1() {
			return dep1;
		}

		@Override
		public IGuiceDependency2 getDep2() {
			return dep2;
		}

		@Override
		public IGuiceDependency3 getDep3() {
			return dep3;
		}
	}
}
