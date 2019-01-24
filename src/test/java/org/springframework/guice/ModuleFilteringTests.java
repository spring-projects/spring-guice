package org.springframework.guice;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.ModuleFilteringTests.FilterThisModule;
import org.springframework.guice.annotation.EnableGuiceModules;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public class ModuleFilteringTests {

	@AfterClass
	public static void cleanUp() {
		System.clearProperty("spring.guice.modules.exclude");
	}

	@Test
	public void verifyAllIsWellWhenNoModulesFiltered() {
		System.setProperty("spring.guice.modules.exclude", "FilterSomeNonExistentModule");
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ModuleFilteringTestsConfig.class);
		SomeInterface someDependency = context.getBean(SomeInterface.class);
		assertNotNull(someDependency);
		context.close();
	}

	@Test(expected=NoSuchBeanDefinitionException.class)
	public void verifyFilteredModuleIsFiltered() {
		System.setProperty("spring.guice.modules.exclude", "FilterThisModule");
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ModuleFilteringTestsConfig.class);
		try {
		context.getBean(SomeInterface.class);
		} finally {
			context.close();
		}
	}

	public static interface SomeInterface {}
	
	public static class SomeDependency implements SomeInterface {
	}
	
	public static class FilterThisModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(SomeInterface.class).to(SomeDependency.class).asEagerSingleton();
		}
	}
}

@EnableGuiceModules
@Configuration
class ModuleFilteringTestsConfig {
	
	@Bean
	public Module module() {
		return new AbstractModule() {
			
			@Override
			protected void configure() {
				install(new FilterThisModule());		
			}
		};
	}
}