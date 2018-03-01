package org.springframework.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import org.junit.AfterClass;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.BindingDeduplicationTests.SomeDependency;
import org.springframework.guice.annotation.EnableGuiceModules;

import static org.junit.Assert.assertNotNull;

public class BindingDeduplicationTests {

	@AfterClass
	public static void cleanUp() {
		System.clearProperty("spring.guice.dedup");
	}

	@Test
	public void verifyNoDuplicateBindingErrorWhenDedupeEnabled() {
		System.setProperty("spring.guice.dedup", "true");
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BindingDeduplicationTestsConfig.class);
		SomeDependency someDependency = context.getBean(SomeDependency.class);
		assertNotNull(someDependency);
		context.close();
	}

	@Test(expected = BeanCreationException.class)
	public void verifyDuplicateBindingErrorWhenDedupeNotEnabled() {
		System.setProperty("spring.guice.dedup", "false");
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BindingDeduplicationTestsConfig.class);
		context.close();
	}

	public static class SomeDependency {
	}

}

@EnableGuiceModules
@Configuration
class BindingDeduplicationTestsConfig {

	@Bean
	public SomeDependency stringBean() {
		return new SomeDependency();
	}

	@Bean
	public Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(SomeDependency.class).asEagerSingleton();
			}
		};
	}
}