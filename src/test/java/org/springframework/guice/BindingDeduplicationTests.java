package org.springframework.guice;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.BindingDeduplicationTests.SomeDependency;
import org.springframework.guice.annotation.EnableGuiceModules;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Module;

public class BindingDeduplicationTests {
	
	@Test
	public void verifyNoDuplicateBindingErrorWhenDedupeEnabled() {	
		System.setProperty("spring.guice.dedupeBindings", "true");
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(BindingDeduplicationTestsConfig.class);
		SomeDependency someDependency = context.getBean(SomeDependency.class);
		assertNotNull(someDependency);
		context.close();
	}
	
	@Test(expected=CreationException.class)
	public void verifyDuplicateBindingErrorWhenDedupeNotEnabled() {	
		System.setProperty("spring.guice.dedupeBindings", "false");
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(BindingDeduplicationTestsConfig.class);
		context.close();
	}
	
	public static class SomeDependency {}
	
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