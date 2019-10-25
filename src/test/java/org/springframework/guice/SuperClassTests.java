package org.springframework.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.ResolvableType;
import org.springframework.guice.annotation.EnableGuiceModules;
import org.springframework.stereotype.Component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configClass);
		assertTrue(context.getBean(IParent.class) instanceof IGrandChildImpl);
		assertTrue(context.getBean(IChild.class) instanceof IGrandChildImpl);
		assertTrue(context.getBean(IGrandChild.class) instanceof IGrandChildImpl);
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
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configClass);
		Injector injector = context.getBean(Injector.class);
		assertTrue(injector.getInstance(IParent.class) instanceof IGrandChildImpl);
		assertTrue(injector.getInstance(IChild.class) instanceof IGrandChildImpl);
		assertTrue(injector.getInstance(IGrandChild.class) instanceof IGrandChildImpl);
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
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configClass);

		String[] allParentBeanNames = context.getBeanNamesForType(IParentWithType.class);
		assertEquals(2, allParentBeanNames.length);

		String[] stringParentBeanNames = context.getBeanNamesForType(
				ResolvableType.forClassWithGenerics(IParentWithType.class, String.class));
		assertEquals(1, stringParentBeanNames.length);
		assertTrue(new TypeLiteral<IGrandChildWithType<String>>() {
		}.getRawType().isInstance(context.getBean(stringParentBeanNames[0])));

		String[] integerParentBeanNames = context.getBeanNamesForType(ResolvableType
				.forClassWithGenerics(IParentWithType.class, Integer.class));
		assertEquals(1, integerParentBeanNames.length);
		assertTrue(new TypeLiteral<IGrandChildWithType<Integer>>() {
		}.getRawType().isInstance(context.getBean(integerParentBeanNames[0])));

		String[] allChildBeanNames = context.getBeanNamesForType(IChildWithType.class);
		assertEquals(2, allChildBeanNames.length);

		String[] stringChildBeanNames = context.getBeanNamesForType(
				ResolvableType.forClassWithGenerics(IChildWithType.class, String.class));
		assertEquals(1, stringChildBeanNames.length);
		assertTrue(new TypeLiteral<IChildWithType<String>>() {
		}.getRawType().isInstance(context.getBean(stringChildBeanNames[0])));

		String[] integerChildBeanNames = context.getBeanNamesForType(
				ResolvableType.forClassWithGenerics(IChildWithType.class, Integer.class));
		assertEquals(1, integerChildBeanNames.length);
		assertTrue(new TypeLiteral<IChildWithType<Integer>>() {
		}.getRawType().isInstance(context.getBean(integerChildBeanNames[0])));

		String[] allGrandChildBeanNames = context
				.getBeanNamesForType(IGrandChildWithType.class);
		assertEquals(2, allGrandChildBeanNames.length);

		String[] stringGrandChildBeanNames = context.getBeanNamesForType(ResolvableType
				.forClassWithGenerics(IGrandChildWithType.class, String.class));
		assertEquals(1, stringGrandChildBeanNames.length);
		assertTrue(new TypeLiteral<IGrandChildWithType<String>>() {
		}.getRawType().isInstance(context.getBean(stringGrandChildBeanNames[0])));

		String[] integerGrandChildBeanNames = context.getBeanNamesForType(ResolvableType
				.forClassWithGenerics(IGrandChildWithType.class, Integer.class));
		assertEquals(1, integerGrandChildBeanNames.length);
		assertTrue(new TypeLiteral<IGrandChildWithType<Integer>>() {
		}.getRawType().isInstance(context.getBean(integerGrandChildBeanNames[0])));

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
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configClass);
		Injector injector = context.getBean(Injector.class);
		IParentWithType<String> iParentString = injector
				.getInstance(Key.get(new TypeLiteral<IParentWithType<String>>() {
				}));
		assertTrue(iParentString instanceof IGrandChildString);
		IParentWithType<Integer> iParentInteger = injector
				.getInstance(Key.get(new TypeLiteral<IParentWithType<Integer>>() {
				}));
		assertTrue(iParentInteger instanceof IGrandChildInteger);

		IChildWithType<String> iChildString = injector
				.getInstance(Key.get(new TypeLiteral<IChildWithType<String>>() {
				}));
		assertTrue(iChildString instanceof IGrandChildString);
		IChildWithType<Integer> iChildInteger = injector
				.getInstance(Key.get(new TypeLiteral<IChildWithType<Integer>>() {
				}));
		assertTrue(iChildInteger instanceof IGrandChildInteger);

		IGrandChildWithType<String> iGrandChildString = injector
				.getInstance(Key.get(new TypeLiteral<IGrandChildWithType<String>>() {
				}));
		assertTrue(iGrandChildString instanceof IGrandChildString);
		IGrandChildWithType<Integer> iGrandChildInteger = injector
				.getInstance(Key.get(new TypeLiteral<IGrandChildWithType<Integer>>() {
				}));
		assertTrue(iGrandChildInteger instanceof IGrandChildInteger);
	}

	@SuppressWarnings("resource")
	@Test
	public void testSpringClass() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ModulesConfig.class);
		IFoo iFoo = context.getBean(IFoo.class);
		assertTrue(iFoo instanceof Foo);
		assertTrue(iFoo instanceof SubFoo);

		Foo foo = context.getBean(Foo.class);
		assertTrue(foo instanceof SubFoo);
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
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configClass);
		Injector injector = context.getBean(Injector.class);
		IFoo iFoo = injector.getInstance(IFoo.class);
		assertTrue(iFoo instanceof Foo);
		assertTrue(iFoo instanceof SubFoo);

		Foo foo = injector.getInstance(Foo.class);
		assertTrue(foo instanceof SubFoo);
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
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configClass);
		String[] stringBeanNames = context.getBeanNamesForType(
				ResolvableType.forClassWithGenerics(IFooWithType.class, String.class));
		assertEquals(1, stringBeanNames.length);
		assertTrue(context.getBean(stringBeanNames[0]) instanceof StringFoo);
		assertTrue(context.getBean(stringBeanNames[0]) instanceof SubStringFoo);

		stringBeanNames = context.getBeanNamesForType(StringFoo.class);
		assertEquals(1, stringBeanNames.length);
		assertTrue(context.getBean(stringBeanNames[0]) instanceof StringFoo);
		assertTrue(context.getBean(stringBeanNames[0]) instanceof SubStringFoo);

		String[] integerBeanNames = context.getBeanNamesForType(
				ResolvableType.forClassWithGenerics(IFooWithType.class, Integer.class));
		assertEquals(1, integerBeanNames.length);
		assertTrue(context.getBean(integerBeanNames[0]) instanceof IntegerFoo);
		assertTrue(context.getBean(integerBeanNames[0]) instanceof SubIntegerFoo);

		integerBeanNames = context.getBeanNamesForType(IntegerFoo.class);
		assertEquals(1, integerBeanNames.length);
		assertTrue(context.getBean(integerBeanNames[0]) instanceof IntegerFoo);
		assertTrue(context.getBean(integerBeanNames[0]) instanceof SubIntegerFoo);
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
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configClass);
		Injector injector = context.getBean(Injector.class);

		IFooWithType<String> iFooWithTypeString = injector
				.getInstance(Key.get(new TypeLiteral<IFooWithType<String>>() {
				}));
		assertTrue(iFooWithTypeString instanceof StringFoo);
		assertTrue(iFooWithTypeString instanceof SubStringFoo);

		StringFoo stringFoo = injector.getInstance(StringFoo.class);
		assertTrue(stringFoo instanceof SubStringFoo);

		IFooWithType<Integer> iFooWithTypeInteger = injector
				.getInstance(Key.get(new TypeLiteral<IFooWithType<Integer>>() {
				}));
		assertTrue(iFooWithTypeInteger instanceof IntegerFoo);
		assertTrue(iFooWithTypeInteger instanceof SubIntegerFoo);

		IntegerFoo integerFoo = injector.getInstance(IntegerFoo.class);
		assertTrue(integerFoo instanceof SubIntegerFoo);
	}

	static class DisableJITConfig {
		@Bean
		public AbstractModule disableJITModule() {
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
		public IGrandChild iGrandChild() {
			return new IGrandChildImpl();
		}

		@Bean
		public IGrandChildWithType<String> iChildString() {
			return new IGrandChildString();
		}

		@Bean
		public IGrandChildWithType<Integer> iChildInteger() {
			return new IGrandChildInteger();
		}

		@Bean
		public SubFoo subFoo() {
			return new SubFoo();
		}

		@Bean
		public SubStringFoo stringFoo() {
			return new SubStringFoo();
		}

		@Bean
		SubIntegerFoo integerFoo() {
			return new SubIntegerFoo();
		}

	}

	@Configuration
	@EnableGuiceModules
	@Import({ IGrandChildImpl.class, IGrandChildString.class, IGrandChildInteger.class,
			SubFoo.class, SubStringFoo.class, SubIntegerFoo.class })
	static class ImportConfig extends DisableJITConfig {
	}

	@Configuration
	@EnableGuiceModules
	@ComponentScan(basePackageClasses = ComponentScanConfig.class, resourcePattern = "**/SuperClassTests**.class", excludeFilters = {
			@ComponentScan.Filter(Configuration.class) })
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

}
