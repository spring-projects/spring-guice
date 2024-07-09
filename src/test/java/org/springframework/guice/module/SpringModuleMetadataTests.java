/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.guice.module;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.name.Names;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 *
 */
public class SpringModuleMetadataTests {

	@Test
	public void twoConfigClasses() throws Exception {
		Injector injector = createInjector(TestConfig.class, OtherConfig.class);
		assertThat(injector.getInstance(Service.class)).isNotNull();
	}

	@Test
	public void twoServices() throws Exception {
		Injector injector = createInjector(TestConfig.class, MoreConfig.class);
		assertThatExceptionOfType(ProvisionException.class)
				.isThrownBy(() -> assertThat(injector.getInstance(Service.class)).isNotNull());
	}

	@Test
	public void twoServicesOnePrimary() throws Exception {
		Injector injector = createInjector(TestConfig.class, PrimaryConfig.class);
		assertThat(injector.getInstance(Service.class)).isNotNull();
	}

	@Test
	public void twoServicesByName() throws Exception {
		Injector injector = createInjector(TestConfig.class, MoreConfig.class);
		assertThat(injector.getInstance(Key.get(Service.class, Names.named("service")))).isNotNull();
	}

	@Test
	public void threeServicesByQualifier() throws Exception {
		Injector injector = createInjector(PrimaryConfig.class, QualifiedConfig.class);

		assertThat(injector.getInstance(
				Key.get(Service.class, ServiceQualifierAnnotated.class.getAnnotation(ServiceQualifier.class))))
						.extracting("name").isEqualTo("emptyQualifierService");

		assertThat(injector.getInstance(
				Key.get(Service.class, EmptyServiceQualifierAnnotated.class.getAnnotation(ServiceQualifier.class))))
						.extracting("name").isEqualTo("emptyQualifierService");

		assertThat(injector.getInstance(
				Key.get(Service.class, MyServiceQualifierAnnotated.class.getAnnotation(ServiceQualifier.class))))
						.extracting("name").isEqualTo("myService");

		assertThat(injector.getInstance(Key.get(Service.class, Names.named("namedService")))).extracting("name")
				.isEqualTo("namedService");

		assertThat(injector.getInstance(Key.get(Service.class, Names.named("namedServiceWithADifferentBeanName"))))
				.extracting("name").isEqualTo("namedServiceWithADifferentBeanName");

		assertThat(injector.getInstance(Service.class)).extracting("name").isEqualTo("primary");

		// Test cases where we don't expect to find any bindings
		assertThatCode(() -> injector.getInstance(Key.get(Service.class, Names.named("randomService"))))
				.isInstanceOf(ConfigurationException.class);

		assertThatCode(() -> injector.getInstance(
				Key.get(Service.class, NoServiceQualifierAnnotated.class.getAnnotation(ServiceQualifier.class))))
						.isInstanceOf(ConfigurationException.class);

		assertThatCode(() -> injector.getInstance(Key.get(Service.class, UnboundServiceQualifier.class)))
				.isInstanceOf(ConfigurationException.class);

	}

	@Test
	public void includes() throws Exception {
		Injector injector = createInjector(TestConfig.class, MetadataIncludesConfig.class);
		assertThatExceptionOfType(ConfigurationException.class)
				.isThrownBy(() -> assertThat(injector.getBinding(Service.class)).isNull());
	}

	@Test
	public void excludes() throws Exception {
		Injector injector = createInjector(TestConfig.class, MetadataExcludesConfig.class);
		assertThatExceptionOfType(ConfigurationException.class)
				.isThrownBy(() -> assertThat(injector.getBinding(Service.class)).isNull());
	}

	private Injector createInjector(Class<?>... config) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(config);
		context.refresh();
		return Guice.createInjector(new SpringModule(context));
	}

	interface Service {

		String getName();

	}

	protected static class MyService implements Service {

		private final String name;

		protected MyService(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}

	}

	public static class Foo {

		@Inject
		public Foo(Service service) {
		}

	}

	@Configuration
	protected static class MetadataExcludesConfig {

		@Bean
		public GuiceModuleMetadata guiceModuleMetadata() {
			GuiceModuleMetadata metadata = new GuiceModuleMetadata();
			metadata.exclude(new AssignableTypeFilter(Service.class));
			return metadata;
		}

	}

	@Configuration
	protected static class MetadataIncludesConfig {

		@Bean
		public GuiceModuleMetadata guiceModuleMetadata() {
			GuiceModuleMetadata metadata = new GuiceModuleMetadata();
			metadata.include(new AnnotationTypeFilter(Cacheable.class));
			return metadata;
		}

	}

	@Configuration
	public static class TestConfig {

		@Bean
		public Service service() {
			return new MyService("service");
		}

	}

	@Configuration
	public static class PrimaryConfig {

		@Bean
		@Primary
		public Service primary() {
			return new MyService("primary");
		}

	}

	@Configuration
	public static class MoreConfig {

		@Bean
		public Service more() {
			return new MyService("more");
		}

	}

	@Configuration
	public static class QualifiedConfig {

		@Bean
		@Named("namedService")
		public Service namedService() {
			return new MyService("namedService");
		}

		@Bean
		@Named("namedServiceWithADifferentBeanName")
		public Service anotherNamedService() {
			return new MyService("namedServiceWithADifferentBeanName");
		}

		@Bean
		@ServiceQualifier
		public Service emptyQualifierService() {
			return new MyService("emptyQualifierService");
		}

		@Bean
		@ServiceQualifier(type = "myService")
		public Service myService(@Named("namedService") Service service) {
			return new MyService("myService");
		}

	}

	@Configuration
	public static class OtherConfig {

	}

	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	@interface ServiceQualifier {

		String type() default "";

	}

	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	@interface UnboundServiceQualifier {

	}

	@ServiceQualifier
	interface ServiceQualifierAnnotated {

	}

	@ServiceQualifier(type = "")
	interface EmptyServiceQualifierAnnotated {

	}

	@ServiceQualifier(type = "myService")
	interface MyServiceQualifierAnnotated {

	}

	@ServiceQualifier(type = "noService")
	interface NoServiceQualifierAnnotated {

	}

}
