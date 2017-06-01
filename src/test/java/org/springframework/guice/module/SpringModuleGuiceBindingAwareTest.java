package org.springframework.guice.module;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Provider;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.spi.ProvisionListener;
import com.google.inject.util.Providers;

import org.junit.Test;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.AopTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class SpringModuleGuiceBindingAwareTest {

	@Test
	public void testAllDependenciesInjectedAndLifeycleMethodsCalledOnce() {
		Injector injector = Guice.createInjector(new SimpleGuiceModule(),
				new SpringModule(new Provider<ConfigurableListableBeanFactory>() {
					@Override
					public ConfigurableListableBeanFactory get() {
						PartiallyRefreshableApplicationContext context = new PartiallyRefreshableApplicationContext();
						context.register(
								GuiceProjectWithSpringLibraryTestSpringConfig.class);
						context.getBeanFactory().registerSingleton("refreshListener",
								new ContextRefreshingProvisionListener(context));
						context.partialRefresh();
						return context.getBeanFactory();
					}
				}));

		// check guice provided bindings
		assertNotNull(injector.getInstance(GuiceDependency1.class));
		assertNotNull(injector.getInstance(IGuiceDependency1.class));

		// check spring bindings as interface
		ISpringBean springBean = injector.getInstance(ISpringBean.class);
		assertNotNull(springBean);
		assertNotNull(springBean.getDep1());
		assertNotNull(springBean.getDep2());
		assertNotNull(springBean.getDep3());

		// invoke a method to make sure we aren't dealing with a lazy proxy
		assertEquals("done", springBean.getDep1().doWork());

		// check binding equality
		assertSame(injector.getInstance(IGuiceDependency1.class),
				AopTestUtils.getTargetObject(springBean.getDep1()));
		assertSame(injector.getInstance(IGuiceDependency2.class),
				AopTestUtils.getTargetObject(springBean.getDep2()));
		assertSame(injector.getInstance(IGuiceDependency3.class),
				AopTestUtils.getTargetObject(springBean.getDep3()));
	}

	static class SimpleGuiceModule extends AbstractModule {

		@Override
		protected void configure() {
			bind(IGuiceDependency1.class).to(GuiceDependency1.class).in(Scopes.SINGLETON); // test
																							// normal
																							// binding
			bind(IGuiceDependency2.class).toInstance(new IGuiceDependency2() {
			}); // test instance binding
			bind(IGuiceDependency3.class)
					.toProvider(Providers.of(new IGuiceDependency3() {
					})); // test provider binding
		}
	}

	@Configuration
	static class GuiceProjectWithSpringLibraryTestSpringConfig {

		@Bean
		public ISpringBean springDefinedSomething(IGuiceDependency1 dependency) {
			return new SpringBean(dependency);
		}

		@Bean
		public ApplicationListener<ApplicationEvent> eventListener(
				final IGuiceDependency1 dependency) {
			return new ApplicationListener<ApplicationEvent>() {
				@Override
				public void onApplicationEvent(ApplicationEvent event) {
					dependency.doWork();
				}
			};
		}
	}

	static interface IGuiceDependency1 {
		String doWork();
	}

	static interface IGuiceDependency2 {
	}

	static interface IGuiceDependency3 {
	}

	static class GuiceDependency1 implements IGuiceDependency1 {
		public String doWork() {
			return "done";
		}
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

	private static final class PartiallyRefreshableApplicationContext
			extends AnnotationConfigApplicationContext {

		private final AtomicBoolean partiallyRefreshed = new AtomicBoolean(false);

		/*
		 * Initializes beanFactoryPostProcessors only to ensure that all BeanDefinition's
		 * are available
		 */
		void partialRefresh() {
			invokeBeanFactoryPostProcessors(getBeanFactory());
		}

		@Override
		protected void invokeBeanFactoryPostProcessors(
				ConfigurableListableBeanFactory beanFactory) {
			if (partiallyRefreshed.compareAndSet(false, true)) {
				super.invokeBeanFactoryPostProcessors(beanFactory);
			}
		}
	}

	private static final class ContextRefreshingProvisionListener
			implements ProvisionListener {
		private final ConfigurableApplicationContext context;
		private final AtomicBoolean initialized = new AtomicBoolean(false);

		private ContextRefreshingProvisionListener(
				ConfigurableApplicationContext context) {
			this.context = context;
		}

		@Override
		public <T> void onProvision(ProvisionInvocation<T> provision) {
			if (!initialized.getAndSet(true) && !context.isActive()) {
				context.refresh();
			}
			provision.provision();
		}
	}

}
