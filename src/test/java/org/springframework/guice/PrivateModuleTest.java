package org.springframework.guice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.PrivateModuleTest.SomeInterface;
import org.springframework.guice.PrivateModuleTest.SomePrivateModule;
import org.springframework.guice.annotation.EnableGuiceModules;

import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.google.inject.name.Names;

public class PrivateModuleTest {

	private static AnnotationConfigApplicationContext context;
	
	@BeforeClass
	public static void init() {
		context = new AnnotationConfigApplicationContext(PrivateModuleTestConfig.class);
	}
	
	@AfterClass
	public static void cleanup() {
		if(context != null) {
			context.close();
		}
	}

	@Test
	public void verifyPrivateModulesCanExposeBindings() {	
		Injector injector = context.getBean(Injector.class);
		SomeInterface injectorProvidedPrivateBinding = injector.getInstance(Key.get(SomeInterface.class, Names.named("exposed")));
		assertNotNull(injectorProvidedPrivateBinding);
		SomeInterface springProvidedPrivateBinding = context.getBean(SomeInterface.class);
		assertNotNull(springProvidedPrivateBinding);
		SomeInterface namedPrivateBinding = context.getBean("exposed",SomeInterface.class);
		assertNotNull(namedPrivateBinding);
		assertEquals(injectorProvidedPrivateBinding, springProvidedPrivateBinding);
		assertEquals(injectorProvidedPrivateBinding, namedPrivateBinding);
		String beanDependingOnPrivateBinding = context.getBean("somethingThatWantsAPrivateBinding", String.class);
		assertNotNull(beanDependingOnPrivateBinding);
		assertEquals("foo", beanDependingOnPrivateBinding);
	}
	
	@Test(expected=ConfigurationException.class)
	public void verifyPrivateModulesPrivateBindingsAreNotExposedViaInjector() {
		Injector injector = context.getBean(Injector.class);
		injector.getInstance(Key.get(SomeInterface.class, Names.named("notexposed")));
	}
	
	@Test(expected=NoSuchBeanDefinitionException.class)
	public void verifyPrivateModulesPrivateBindingsAreNotExposedViaSpring() {
		context.getBean("notexposed",SomeInterface.class);
	}

	public static interface SomeInterface {}
	public static class SomePrivateBinding implements SomeInterface {}
	
	public static class SomePrivateModule extends PrivateModule {
		@Override
		protected void configure() {
			bind(SomeInterface.class).annotatedWith(Names.named("exposed")).to(SomePrivateBinding.class).asEagerSingleton();
			bind(SomeInterface.class).annotatedWith(Names.named("notexposed")).to(SomePrivateBinding.class).asEagerSingleton();
			expose(SomeInterface.class).annotatedWith(Names.named("exposed"));	
		}
	}
}

@EnableGuiceModules
@Configuration
class PrivateModuleTestConfig {
	
	@Bean
	public String somethingThatWantsAPrivateBinding(SomeInterface privateBinding) {
		return "foo";
	}
	
	@Bean
	public Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				install(new SomePrivateModule());
			}
		};
	}
}