/*
 * Copyright 2019-2022 the original author or authors.
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

package org.springframework.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.ResolvableType;
import org.springframework.guice.annotation.EnableGuiceModules;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

public class SuperClassTests {

	@Test
	public void testSpringInterface() {
		baseTestSpringInterface(ModulesConfig.class);
	}

	@Test
	public void testImportSpringInterface() {
		baseTestSpringInterface(ImportConfig.class);
	}

	@Test
	public void testComponentScanSpringInterface() {
		baseTestSpringInterface(ComponentScanConfig.class);
	}

	@SuppressWarnings("resource")
	private void baseTestSpringInterface(Class<?> configClass) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configClass);
		assertThat(context.getBean(IParent.class) instanceof IGrandChildImpl).isTrue();
		assertThat(context.getBean(IChild.class) instanceof IGrandChildImpl).isTrue();
		assertThat(context.getBean(IGrandChild.class) instanceof IGrandChildImpl).isTrue();
	}

	@Test
	public void testGuiceInterface() {
		baseTestGuiceInterface(ModulesConfig.class);
	}

	@Test
	public void testImportGuiceInterface() {
		baseTestGuiceInterface(ImportConfig.class);
	}

	@Test
	public void testComponentScanGuiceInterface() {
		baseTestGuiceInterface(ComponentScanConfig.class);
	}

	@SuppressWarnings("resource")
	private void baseTestGuiceInterface(Class<?> configClass) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configClass);
		Injector injector = context.getBean(Injector.class);
		assertThat(injector.getInstance(IParent.class) instanceof IGrandChildImpl).isTrue();
		assertThat(injector.getInstance(IChild.class) instanceof IGrandChildImpl).isTrue();
		assertThat(injector.getInstance(IGrandChild.class) instanceof IGrandChildImpl).isTrue();
	}

	@Test
	public void testSpringInterfaceWithType() {
		baseTestSpringInterfaceWithType(ModulesConfig.class);
	}

	@Test
	public void testImportSpringInterfaceWithType() {
		baseTestSpringInterfaceWithType(ImportConfig.class);
	}

	@Test
	public void testComponentScanSpringInterfaceWithType() {
		baseTestSpringInterfaceWithType(ComponentScanConfig.class);
	}

	@SuppressWarnings("resource")
	private void baseTestSpringInterfaceWithType(Class<?> configClass) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configClass);

		String[] allParentBeanNames = context.getBeanNamesForType(IParentWithType.class);
		assertThat(allParentBeanNames.length).isEqualTo(2);

		String[] stringParentBeanNames = context
				.getBeanNamesForType(ResolvableType.forClassWithGenerics(IParentWithType.class, String.class));
		assertThat(stringParentBeanNames.length).isEqualTo(1);
		assertThat(new TypeLiteral<IGrandChildWithType<String>>() {
		}.getRawType().isInstance(context.getBean(stringParentBeanNames[0]))).isTrue();

		String[] integerParentBeanNames = context
				.getBeanNamesForType(ResolvableType.forClassWithGenerics(IParentWithType.class, Integer.class));
		assertThat(integerParentBeanNames.length).isEqualTo(1);
		assertThat(new TypeLiteral<IGrandChildWithType<Integer>>() {
		}.getRawType().isInstance(context.getBean(integerParentBeanNames[0]))).isTrue();

		String[] allChildBeanNames = context.getBeanNamesForType(IChildWithType.class);
		assertThat(allChildBeanNames.length).isEqualTo(2);

		String[] stringChildBeanNames = context
				.getBeanNamesForType(ResolvableType.forClassWithGenerics(IChildWithType.class, String.class));
		assertThat(stringChildBeanNames.length).isEqualTo(1);
		assertThat(new TypeLiteral<IChildWithType<String>>() {
		}.getRawType().isInstance(context.getBean(stringChildBeanNames[0]))).isTrue();

		String[] integerChildBeanNames = context
				.getBeanNamesForType(ResolvableType.forClassWithGenerics(IChildWithType.class, Integer.class));
		assertThat(integerChildBeanNames.length).isEqualTo(1);
		assertThat(new TypeLiteral<IChildWithType<Integer>>() {
		}.getRawType().isInstance(context.getBean(integerChildBeanNames[0]))).isTrue();

		String[] allGrandChildBeanNames = context.getBeanNamesForType(IGrandChildWithType.class);
		assertThat(allGrandChildBeanNames.length).isEqualTo(2);

		String[] stringGrandChildBeanNames = context
				.getBeanNamesForType(ResolvableType.forClassWithGenerics(IGrandChildWithType.class, String.class));
		assertThat(stringGrandChildBeanNames.length).isEqualTo(1);
		assertThat(new TypeLiteral<IGrandChildWithType<String>>() {
		}.getRawType().isInstance(context.getBean(stringGrandChildBeanNames[0]))).isTrue();

		String[] integerGrandChildBeanNames = context
				.getBeanNamesForType(ResolvableType.forClassWithGenerics(IGrandChildWithType.class, Integer.class));
		assertThat(integerGrandChildBeanNames.length).isEqualTo(1);
		assertThat(new TypeLiteral<IGrandChildWithType<Integer>>() {
		}.getRawType().isInstance(context.getBean(integerGrandChildBeanNames[0]))).isTrue();

	}

	@Test
	public void testGuiceInterfaceWithType() {
		baseTestGuiceInterfaceWithType(ModulesConfig.class);
	}

	@Test
	public void testImportGuiceInterfaceWithType() {
		baseTestGuiceInterfaceWithType(ImportConfig.class);
	}

	@Test
	public void testComponentScanGuiceInterfaceWithType() {
		baseTestGuiceInterfaceWithType(ComponentScanConfig.class);
	}

	@SuppressWarnings("resource")
	private void baseTestGuiceInterfaceWithType(Class<?> configClass) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configClass);
		Injector injector = context.getBean(Injector.class);
		IParentWithType<String> iParentString = injector
				.getInstance(Key.get(new TypeLiteral<IParentWithType<String>>() {
				}));
		assertThat(iParentString instanceof IGrandChildString).isTrue();
		IParentWithType<Integer> iParentInteger = injector
				.getInstance(Key.get(new TypeLiteral<IParentWithType<Integer>>() {
				}));
		assertThat(iParentInteger instanceof IGrandChildInteger).isTrue();

		IChildWithType<String> iChildString = injector.getInstance(Key.get(new TypeLiteral<IChildWithType<String>>() {
		}));
		assertThat(iChildString instanceof IGrandChildString).isTrue();
		IChildWithType<Integer> iChildInteger = injector
				.getInstance(Key.get(new TypeLiteral<IChildWithType<Integer>>() {
				}));
		assertThat(iChildInteger instanceof IGrandChildInteger).isTrue();

		IGrandChildWithType<String> iGrandChildString = injector
				.getInstance(Key.get(new TypeLiteral<IGrandChildWithType<String>>() {
				}));
		assertThat(iGrandChildString instanceof IGrandChildString).isTrue();
		IGrandChildWithType<Integer> iGrandChildInteger = injector
				.getInstance(Key.get(new TypeLiteral<IGrandChildWithType<Integer>>() {
				}));
		assertThat(iGrandChildInteger instanceof IGrandChildInteger).isTrue();
	}

	@SuppressWarnings("resource")
	@Test
	public void testSpringClass() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ModulesConfig.class);
		IFoo iFoo = context.getBean(IFoo.class);
		assertThat(iFoo instanceof Foo).isTrue();
		assertThat(iFoo instanceof SubFoo).isTrue();

		Foo foo = context.getBean(Foo.class);
		assertThat(foo instanceof SubFoo).isTrue();
	}

	@Test
	public void testGuiceClass() {
		baseTestGuiceClass(ModulesConfig.class);
	}

	@Test
	public void testImportGuiceClass() {
		baseTestGuiceClass(ImportConfig.class);
	}

	@Test
	public void testComponentScanGuiceClass() {
		baseTestGuiceClass(ComponentScanConfig.class);
	}

	@SuppressWarnings("resource")
	private void baseTestGuiceClass(Class<?> configClass) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configClass);
		Injector injector = context.getBean(Injector.class);
		IFoo iFoo = injector.getInstance(IFoo.class);
		assertThat(iFoo instanceof Foo).isTrue();
		assertThat(iFoo instanceof SubFoo).isTrue();

		Foo foo = injector.getInstance(Foo.class);
		assertThat(foo instanceof SubFoo).isTrue();
	}

	@Test
	public void testSpringClassWithType() {
		baseTestSpringClassWithType(ModulesConfig.class);
	}

	@Test
	public void testImportSpringClassWithType() {
		baseTestSpringClassWithType(ImportConfig.class);
	}

	@Test
	public void testComponentScanSpringClassWithType() {
		baseTestSpringClassWithType(ComponentScanConfig.class);
	}

	@SuppressWarnings("resource")
	private void baseTestSpringClassWithType(Class<?> configClass) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configClass);
		String[] stringBeanNames = context
				.getBeanNamesForType(ResolvableType.forClassWithGenerics(IFooWithType.class, String.class));
		assertThat(stringBeanNames.length).isEqualTo(1);
		assertThat(context.getBean(stringBeanNames[0]) instanceof StringFoo).isTrue();
		assertThat(context.getBean(stringBeanNames[0]) instanceof SubStringFoo).isTrue();

		stringBeanNames = context.getBeanNamesForType(StringFoo.class);
		assertThat(stringBeanNames.length).isEqualTo(1);
		assertThat(context.getBean(stringBeanNames[0]) instanceof StringFoo).isTrue();
		assertThat(context.getBean(stringBeanNames[0]) instanceof SubStringFoo).isTrue();

		String[] integerBeanNames = context
				.getBeanNamesForType(ResolvableType.forClassWithGenerics(IFooWithType.class, Integer.class));
		assertThat(integerBeanNames.length).isEqualTo(1);
		assertThat(context.getBean(integerBeanNames[0]) instanceof IntegerFoo).isTrue();
		assertThat(context.getBean(integerBeanNames[0]) instanceof SubIntegerFoo).isTrue();

		integerBeanNames = context.getBeanNamesForType(IntegerFoo.class);
		assertThat(integerBeanNames.length).isEqualTo(1);
		assertThat(context.getBean(integerBeanNames[0]) instanceof IntegerFoo).isTrue();
		assertThat(context.getBean(integerBeanNames[0]) instanceof SubIntegerFoo).isTrue();
	}

	@Test
	public void testGuiceClassWithType() {
		baseTestGuiceClassWithType(ModulesConfig.class);
	}

	@Test
	public void testImportGuiceClassWithType() {
		baseTestGuiceClassWithType(ImportConfig.class);
	}

	@Test
	public void testComponentScanGuiceClassWithType() {
		baseTestGuiceClassWithType(ComponentScanConfig.class);
	}

	@SuppressWarnings("resource")
	private void baseTestGuiceClassWithType(Class<?> configClass) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configClass);
		Injector injector = context.getBean(Injector.class);

		IFooWithType<String> iFooWithTypeString = injector.getInstance(Key.get(new TypeLiteral<IFooWithType<String>>() {
		}));
		assertThat(iFooWithTypeString instanceof StringFoo).isTrue();
		assertThat(iFooWithTypeString instanceof SubStringFoo).isTrue();

		StringFoo stringFoo = injector.getInstance(StringFoo.class);
		assertThat(stringFoo instanceof SubStringFoo).isTrue();

		IFooWithType<Integer> iFooWithTypeInteger = injector
				.getInstance(Key.get(new TypeLiteral<IFooWithType<Integer>>() {
				}));
		assertThat(iFooWithTypeInteger instanceof IntegerFoo).isTrue();
		assertThat(iFooWithTypeInteger instanceof SubIntegerFoo).isTrue();

		IntegerFoo integerFoo = injector.getInstance(IntegerFoo.class);
		assertThat(integerFoo instanceof SubIntegerFoo).isTrue();
	}

	@Test
	public void testSpringFactoryBean() {
		baseTestSpringFactoryBean(ModulesConfig.class);
	}

	@Test
	public void testImportSpringFactoryBean() {
		baseTestSpringFactoryBean(ImportConfig.class);
	}

	@Test
	public void testComponentScanSpringFactoryBean() {
		baseTestSpringFactoryBean(ComponentScanConfig.class);
	}

	private void baseTestSpringFactoryBean(Class<?> configClass) {
		@SuppressWarnings("resource")
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configClass);

		Bar bar = context.getBean(Bar.class);
		assertThat(bar instanceof Bar).isTrue();
	}

	@Test
	public void testGuiceFactoryBean() {
		baseTestGuiceFactoryBean(ModulesConfig.class);
	}

	@Test
	public void testImportGuiceFactoryBean() {
		baseTestGuiceFactoryBean(ImportConfig.class);
	}

	@Test
	public void testComponentScanGuiceFactoryBean() {
		baseTestGuiceFactoryBean(ComponentScanConfig.class);
	}

	private void baseTestGuiceFactoryBean(Class<?> configClass) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configClass);
		Injector injector = context.getBean(Injector.class);
		Bar bar = injector.getInstance(Bar.class);
		assertThat(bar instanceof Bar).isTrue();
	}

	static class DisableJITConfig {

		@Bean
		AbstractModule disableJITModule() {
			return new AbstractModule() {
				@Override
				protected void configure() {
					binder().requireExplicitBindings();
				}
			};
		}

	}

	@Configuration
	@EnableGuiceModules
	static class ModulesConfig extends DisableJITConfig {

		@Bean
		IGrandChild iGrandChild() {
			return new IGrandChildImpl();
		}

		@Bean
		IGrandChildWithType<String> iChildString() {
			return new IGrandChildString();
		}

		@Bean
		IGrandChildWithType<Integer> iChildInteger() {
			return new IGrandChildInteger();
		}

		@Bean
		SubFoo subFoo() {
			return new SubFoo();
		}

		@Bean
		SubStringFoo stringFoo() {
			return new SubStringFoo();
		}

		@Bean
		SubIntegerFoo integerFoo() {
			return new SubIntegerFoo();
		}

		@Bean
		BarFactory barFactory() {
			return new BarFactory();
		}

	}

	@Configuration
	@EnableGuiceModules
	@Import({ IGrandChildImpl.class, IGrandChildString.class, IGrandChildInteger.class, SubFoo.class,
			SubStringFoo.class, SubIntegerFoo.class, BarFactory.class })
	static class ImportConfig extends DisableJITConfig {

	}

	@Configuration
	@EnableGuiceModules
	@ComponentScan(basePackageClasses = ComponentScanConfig.class, resourcePattern = "**/SuperClassTests**.class",
			excludeFilters = { @ComponentScan.Filter(Configuration.class) })
	static class ComponentScanConfig extends DisableJITConfig {

	}

	public interface IParent {

	}

	public interface IChild extends IParent {

	}

	public interface IGrandChild extends IChild {

	}

	@Component
	public static class IGrandChildImpl implements IGrandChild {

	}

	public interface IParentWithType<T> {

	}

	public interface IChildWithType<T> extends IParentWithType<T> {

	}

	public interface IGrandChildWithType<T> extends IChildWithType<T> {

	}

	@Component
	public static class IGrandChildString implements IGrandChildWithType<String> {

	}

	@Component
	public static class IGrandChildInteger implements IGrandChildWithType<Integer> {

	}

	public interface IFoo {

	}

	public static class Foo implements IFoo {

	}

	@Component
	public static class SubFoo extends Foo {

	}

	public interface IFooWithType<T> {

	}

	public static class StringFoo implements IFooWithType<String> {

	}

	@Component
	public static class SubStringFoo extends StringFoo {

	}

	public static class IntegerFoo implements IFooWithType<Integer> {

	}

	@Component
	public static class SubIntegerFoo extends IntegerFoo {

	}

	public static class Bar {

	}

	@Component
	public static class BarFactory implements FactoryBean<Bar> {

		@Override
		public Bar getObject() {
			return new Bar();
		}

		@Override
		public Class<?> getObjectType() {
			return Bar.class;
		}

	}

}
