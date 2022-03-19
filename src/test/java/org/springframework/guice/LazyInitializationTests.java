package org.springframework.guice;

import com.google.inject.AbstractModule;
import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.guice.annotation.EnableGuiceModules;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LazyInitializationTests {

	@Test
	public void lazyAnnotationIsRespectedOnInjectionPointForGuiceBinding() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(TestConfig.class);
		context.register(GuiceConfig.class);
		context.refresh();

		Service service = context.getBean(Service.class);

		assertTrue(AopUtils.isAopProxy(service.getBean()));
		assertNotNull(context.getBean(TestBean.class));
	}

	@Test
	public void lazyAnnotationIsRespectedOnInjectionPointForSpringBinding() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(TestConfig.class);
		context.register(SpringConfig.class);
		context.refresh();

		Service service = context.getBean(Service.class);

		assertTrue(AopUtils.isAopProxy(service.getBean()));
		assertNotNull(context.getBean(TestBean.class));
	}

	@Configuration
	@EnableGuiceModules
	static class TestConfig {

		@Bean
		public Service service(@Lazy TestBean bean) {
			return new Service(bean);
		}

	}

	@Configuration
	static class GuiceConfig {

		@Bean
		public GuiceModule guiceModule() {
			return new GuiceModule();
		}

	}

	@Configuration
	static class SpringConfig {

		@Bean
		public TestBean testBean() {
			return new TestBean();
		}

	}

	static class GuiceModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(TestBean.class).asEagerSingleton();
		}

	}

	static class Service {

		private final TestBean bean;

		public Service(TestBean bean) {
			this.bean = bean;
		}

		public TestBean getBean() {
			return bean;
		}

	}

	static class TestBean {

	}

}
