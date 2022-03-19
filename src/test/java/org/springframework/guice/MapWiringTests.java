package org.springframework.guice;

import java.util.Map;

import com.google.inject.AbstractModule;
import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

import static org.junit.Assert.assertTrue;

/**
 * Test injecting Map
 *
 * @author Dave Syer
 */
public class MapWiringTests {

	// Test Guice -> Spring direction
	@SuppressWarnings({ "resource", "unused" })
	@Test
	public void testProvidesMap() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ModulesConfig.class,
				FooBar.class);
		Bar bar = context.getBean(Bar.class);
	}

	@Configuration
	@EnableGuiceModules
	static class ModulesConfig {

		@Bean
		TestConfig testConfig() {
			return new TestConfig();
		}

	}

	@Configuration
	static class FooBar {

		@Bean
		Bar foo(Map<String, Foo> foos) {
			assertTrue(!foos.isEmpty());
			return new Bar();
		}

	}

	static class TestConfig extends AbstractModule {

		@Override
		protected void configure() {
			bind(Foo.class);
		}

	}

	static class Foo {

	}

	static class Bar {

	}

}
