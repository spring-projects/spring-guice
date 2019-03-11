package org.springframework.guice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.ScopingTests.CustomScope;
import org.springframework.guice.ScopingTests.SomeCustomScopeDependency;
import org.springframework.guice.ScopingTests.SomeNoScopeDependency;
import org.springframework.guice.ScopingTests.SomeSingletonDependency;
import org.springframework.guice.annotation.EnableGuiceModules;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scope;

public class ScopingTests {

	@Test
	public void verifyScopes() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ScopingTestsConfig.class);
		SomeSingletonDependency someSingletonDependency1 = context.getBean(SomeSingletonDependency.class);
		SomeSingletonDependency someSingletonDependency2 = context.getBean(SomeSingletonDependency.class);
		
		assertNotNull(someSingletonDependency1);
		assertNotNull(someSingletonDependency2);
		assertEquals(someSingletonDependency1, someSingletonDependency2);
		
		SomeNoScopeDependency someNoScopeDependency1 = context.getBean(SomeNoScopeDependency.class);
		SomeNoScopeDependency someNoScopeDependency2 = context.getBean(SomeNoScopeDependency.class);
		
		assertNotNull(someNoScopeDependency1);
		assertNotNull(someNoScopeDependency2);
		assertNotEquals(someNoScopeDependency1, someNoScopeDependency2);
		
		SomeCustomScopeDependency someCustomScopeDependency1 = context.getBean(SomeCustomScopeDependency.class);
		SomeCustomScopeDependency someCustomScopeDependency2 = context.getBean(SomeCustomScopeDependency.class);
		
		assertNotNull(someCustomScopeDependency1);
		assertNotNull(someCustomScopeDependency2);
		assertNotEquals(someCustomScopeDependency1, someCustomScopeDependency2);
		assertEquals(someCustomScopeDependency1.value, "custom");
		assertEquals(someCustomScopeDependency2.value, "custom");

		context.close();
	}

	public static class SomeSingletonDependency {}
	public static class SomeNoScopeDependency {}
	public static class SomeCustomScopeDependency {
		String value;
		public SomeCustomScopeDependency() {}
		public SomeCustomScopeDependency(String value) {
			this.value=value;
		}
	}
	public interface CustomScope extends Scope {}

}

@EnableGuiceModules
@Configuration
class ScopingTestsConfig {

	@Bean
	public Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				CustomScope customScope = new CustomScope(){
					@SuppressWarnings("unchecked")
					@Override
					public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
						Provider<?> provider = () -> new SomeCustomScopeDependency("custom");
						return (Provider<T>) provider;
					}};
				bind(SomeSingletonDependency.class).asEagerSingleton();
				bind(SomeNoScopeDependency.class);
				bind(SomeCustomScopeDependency.class).in(customScope);
			}
		};
	}
}