package org.springframework.guice;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.ModuleRegistryConfiguration;
import org.springframework.guice.injector.InjectorFactory;

import com.google.inject.Guice;
import com.google.inject.Module;

public class InjectorFactoryTests {

	static final private InjectorFactory injectorFactory = Mockito.mock(InjectorFactory.class);

	@Before
	public void init() {
		Mockito.when(injectorFactory.createInjector(Mockito.anyListOf(Module.class)))
				.thenReturn(Guice.createInjector());
	}

	@Test
	public void testCustomInjectorIsCreated() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(InjectorFactoryConfig.class,
				ModuleRegistryConfiguration.class);
		Mockito.verify(injectorFactory, Mockito.times(1)).createInjector(Mockito.anyListOf(Module.class));
		context.close();
	}

	@Test(expected = ApplicationContextException.class)
	public void testMultipleInjectorFactoriesThrowsApplicationContextException() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(InjectorFactoryConfig.class,
				SecondInjectorFactoryConfig.class, ModuleRegistryConfiguration.class);
		context.close();
	}

	@Configuration
	static class InjectorFactoryConfig {
		@Bean
		public InjectorFactory injectorFactory() {
			return injectorFactory;
		}
	}

	@Configuration
	static class SecondInjectorFactoryConfig {
		@Bean
		public InjectorFactory injectorFactory2() {
			return injectorFactory;
		}
	}
}