package org.springframework.guice;

import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;
import org.springframework.guice.injector.SpringInjector;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class SimpleWiringTests {

	@Test
	public void guiceyFoo() {
		Injector app = Guice.createInjector(new TestConfig());
		assertNotNull(app.getInstance(Foo.class));
	}

	@Test
	public void springyFoo() {
		@SuppressWarnings("resource")
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class,
				MyService.class);
		context.getDefaultListableBeanFactory().registerBeanDefinition(Foo.class.getSimpleName(),
				new RootBeanDefinition(Foo.class));
		assertNotNull(context.getBean(Foo.class));
	}

	@Test
	public void hybridFoo() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class,
				ModulesConfig.class);
		Injector app = new SpringInjector(context);
		assertNotNull(app.getInstance(Foo.class));
	}

	@Configuration
	@EnableGuiceModules
	static class ModulesConfig {

	}

	public static class TestConfig extends AbstractModule {

		@Override
		protected void configure() {
			bind(Service.class).to(MyService.class);
		}

	}

	interface Service {

	}

	public static class MyService implements Service {

	}

	public static class Foo {

		@Inject
		public Foo(Service service) {
		}

	}

}
