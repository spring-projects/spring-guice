package org.springframework.guice;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.DuplicateNamesDifferentTypesTests.SomeJavaxNamedDepWithType1;
import org.springframework.guice.DuplicateNamesDifferentTypesTests.SomeJavaxNamedDepWithType2;
import org.springframework.guice.DuplicateNamesDifferentTypesTests.SomeNamedDepWithType1;
import org.springframework.guice.DuplicateNamesDifferentTypesTests.SomeNamedDepWithType2;
import org.springframework.guice.annotation.EnableGuiceModules;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class DuplicateNamesDifferentTypesTests {

	@Test
	public void verifyNoDuplicateBindingErrorWhenDedupeEnabled() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				DuplicateNamesDifferentTypesTestsConfig.class);

		// Check Guice @Named
		assertNotNull(context.getBean(SomeNamedDepWithType1.class));
		assertNotNull(context.getBean(SomeNamedDepWithType2.class));
		assertNotNull(BeanFactoryAnnotationUtils.qualifiedBeanOfType(context.getBeanFactory(),
				SomeNamedDepWithType1.class, "sameNameDifferentType"));

		// Check javax @Named
		assertNotNull(context.getBean(SomeJavaxNamedDepWithType1.class));
		assertNotNull(context.getBean(SomeJavaxNamedDepWithType2.class));
		assertNotNull(BeanFactoryAnnotationUtils.qualifiedBeanOfType(context.getBeanFactory(),
				SomeJavaxNamedDepWithType1.class, "sameJavaxName"));
		context.getBeansOfType(SomeJavaxNamedDepWithType1.class);

		context.close();
	}

	public static class SomeNamedDepWithType1 {

	}

	public static class SomeNamedDepWithType2 {

	}

	public static class SomeJavaxNamedDepWithType1 {

	}

	public static class SomeJavaxNamedDepWithType2 {

	}

	public static class SomeClassWithDeps {

		@Autowired
		@Qualifier("sameJavaxName2")
		SomeJavaxNamedDepWithType1 qualified;

		@Autowired
		@Named("sameJavaxName2")
		SomeJavaxNamedDepWithType1 named;

		@Autowired
		@javax.inject.Named("sameJavaxName2")
		SomeJavaxNamedDepWithType1 javaxNamed;

	}

}

@EnableGuiceModules
@Configuration
class DuplicateNamesDifferentTypesTestsConfig {

	@Bean
	public Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(SomeNamedDepWithType1.class).annotatedWith(Names.named("sameNameDifferentType"))
						.to(SomeNamedDepWithType1.class);
				bind(SomeNamedDepWithType2.class).annotatedWith(Names.named("sameNameDifferentType"))
						.to(SomeNamedDepWithType2.class);
			}

			@Provides
			@Named("sameJavaxName")
			public SomeJavaxNamedDepWithType1 someJavaxNamedDepWithType1() {
				return new SomeJavaxNamedDepWithType1();
			}

			@Provides
			@Named("sameJavaxName")
			public SomeJavaxNamedDepWithType2 someJavaxNamedDepWithType2() {
				return new SomeJavaxNamedDepWithType2();
			}
		};
	}

}