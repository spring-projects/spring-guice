package org.springframework.guice;

import static org.junit.Assert.assertNotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Named;
import javax.inject.Qualifier;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.BindingAnnotationTests.SomeDependencyWithBindingAnnotationOnProvider;
import org.springframework.guice.BindingAnnotationTests.SomeDependencyWithGuiceNamedAnnotationOnProvider;
import org.springframework.guice.BindingAnnotationTests.SomeDependencyWithNamedAnnotationOnProvider;
import org.springframework.guice.BindingAnnotationTests.SomeDependencyWithQualifierOnProvider;
import org.springframework.guice.BindingAnnotationTests.SomeDependencyWithQualifierOnProviderWhichImplementsSomeInterface;
import org.springframework.guice.BindingAnnotationTests.SomeInterface;
import org.springframework.guice.annotation.EnableGuiceModules;

import com.google.inject.BindingAnnotation;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

public class BindingAnnotationTests {

	@Test
	public void verifyBindingAnnotationsAreRespected() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BindingAnnotationTestsConfig.class);
		Injector injector = context.getBean(Injector.class);
		
		//Check @Qualifier
		SomeDependencyWithQualifierOnProvider someDependencyWithQualifierOnClass = injector.getInstance(Key.get(SomeDependencyWithQualifierOnProvider.class, SomeQualifierAnnotation.class));
		assertNotNull(someDependencyWithQualifierOnClass);
		
		//Check @BindingAnnotaiton
		SomeDependencyWithQualifierOnProvider someDependencyWithBindingAnnotationOnProvider = injector.getInstance(Key.get(SomeDependencyWithQualifierOnProvider.class, SomeQualifierAnnotation.class));
		assertNotNull(someDependencyWithBindingAnnotationOnProvider);
		
		//Check javax @Named
		SomeDependencyWithNamedAnnotationOnProvider someDependencyWithNamedAnnotationOnProvider = injector.getInstance(Key.get(SomeDependencyWithNamedAnnotationOnProvider.class, Names.named("javaxNamed")));
		assertNotNull(someDependencyWithNamedAnnotationOnProvider);
		
		//Check Guice @Named
		SomeDependencyWithGuiceNamedAnnotationOnProvider someDependencyWithGuiceNamedAnnotationOnProvider = injector.getInstance(Key.get(SomeDependencyWithGuiceNamedAnnotationOnProvider.class, Names.named("guiceNamed")));
		assertNotNull(someDependencyWithGuiceNamedAnnotationOnProvider);
		
		//Check @Qualifier with Interface
		SomeInterface someInterface = injector.getInstance(Key.get(SomeInterface.class, SomeQualifierAnnotation.class));
		assertNotNull(someInterface);
		
		context.close();
	}
	

	public static class SomeDependencyWithQualifierOnProvider {}
	public static class SomeDependencyWithBindingAnnotationOnProvider {}
	public static class SomeDependencyWithNamedAnnotationOnProvider {}
	public static class SomeDependencyWithGuiceNamedAnnotationOnProvider {}
	public static interface SomeInterface{}
	public static class SomeDependencyWithQualifierOnProviderWhichImplementsSomeInterface implements SomeInterface {}
}

@Qualifier
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface SomeQualifierAnnotation {}

@BindingAnnotation
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface SomeBindingAnnotation {}


@EnableGuiceModules
@Configuration
class BindingAnnotationTestsConfig {
	
	@Bean
	@SomeQualifierAnnotation
	public SomeDependencyWithQualifierOnProvider someDependencyWithQualifierOnProvider() {
		return new SomeDependencyWithQualifierOnProvider();
	}
	
	@Bean
	@SomeBindingAnnotation
	public SomeDependencyWithBindingAnnotationOnProvider someDependencyWithBindingAnnotationOnProvider() {
		return new SomeDependencyWithBindingAnnotationOnProvider();
	}
	
	@Bean
	@Named("javaxNamed")
	public SomeDependencyWithNamedAnnotationOnProvider someDependencyWithNamedAnnotationOnProvider() {
		return new SomeDependencyWithNamedAnnotationOnProvider();
	}
	
	@Bean
	@com.google.inject.name.Named("guiceNamed")
	public SomeDependencyWithGuiceNamedAnnotationOnProvider someDependencyWithGuiceNamedAnnotationOnProvider() {
		return new SomeDependencyWithGuiceNamedAnnotationOnProvider();
	}
	
	@Bean
	@SomeQualifierAnnotation
	public SomeInterface someInterface() {
		return new SomeDependencyWithQualifierOnProviderWhichImplementsSomeInterface();
	}
}