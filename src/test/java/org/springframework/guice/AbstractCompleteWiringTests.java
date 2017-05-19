package org.springframework.guice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

public abstract class AbstractCompleteWiringTests {

	private Injector injector;

	@Before
	public void init() {
		this.injector = createInjector();
	}

	protected abstract Injector createInjector();

	@Test
	public void injectInstance() {
		Bar bar = new Bar();
		this.injector.injectMembers(bar);
		assertNotNull(bar.service);
	}

	@Test
	public void memberInjector() {
		Bar bar = new Bar();
		this.injector.getMembersInjector(Bar.class).injectMembers(bar);
		assertNotNull(bar.service);
	}

	@Test
	public void getInstanceUnbound() {
		assertNotNull(this.injector.getInstance(Foo.class));
	}

	@Test
	public void getInstanceBound() {
		assertNotNull(this.injector.getInstance(Service.class));
	}

	@Test
	public void getInstanceBoundWithNoInterface() {
		Baz instance = this.injector.getInstance(Baz.class);
		assertNotNull(instance);
		assertEquals(instance, this.injector.getInstance(Baz.class));
	}

	@Test
	public void getProviderUnbound() {
		assertNotNull(this.injector.getProvider(Foo.class).get());
	}

	@Test
	public void getProviderBound() {
		assertNotNull(this.injector.getProvider(Service.class).get());
	}

	@Test
	public void getNamedInstance() {
		assertNotNull(this.injector.getInstance(Key.get(Thang.class, Names.named("thing"))));
	}

	@Test
	public void getNamedInjectedInstance() {
		assertNotNull(this.injector.getInstance(Thing.class).thang);
	}

	public interface Service {
	}

	public static class MyService implements Service {
	}

	public static class Foo {

		@Inject
		public Foo(Service service) {
		}

	}

	public static class Bar {

		private Service service;

		@Inject
		public void setService(Service service) {
			this.service = service;
		}

	}

	public static class Baz {

		@Inject
		public Baz(Service service) {
		}

	}

	public static class Thing {

		private Thang thang;

		@Inject
		public void setThang(@Named("thing") Thang thang) {
			this.thang = thang;
		}

	}

	public static class Thang {
	}
}
