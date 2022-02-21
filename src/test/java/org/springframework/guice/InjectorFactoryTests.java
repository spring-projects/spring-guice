package org.springframework.guice;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;
import org.springframework.guice.annotation.InjectorFactory;

import com.google.inject.Guice;
import com.google.inject.Module;

public class InjectorFactoryTests {

	static final private InjectorFactory injectorFactory = Mockito.mock(InjectorFactory.class);

	@Before
	public void init() {
		Mockito.when(injectorFactory.createInjector(Mockito.anyList()))
				.thenReturn(Guice.createInjector());
	}

	@Test
	public void testCustomInjectorIsCreated() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(InjectorFactoryConfig.class,
				ModulesConfig.class);
		Mockito.verify(injectorFactory, Mockito.times(1)).createInjector(Mockito.anyList());
		context.close();
	}

	@Test(expected = ApplicationContextException.class)
	public void testMultipleInjectorFactoriesThrowsApplicationContextException() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(InjectorFactoryConfig.class,
				SecondInjectorFactoryConfig.class, ModulesConfig.class);
		context.close();
	}

	@Configuration
	@EnableGuiceModules
	static class ModulesConfig {
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