package demo;

import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;

public abstract class AbstractCompleteWiringTests {

	private Injector injector;

	@Before
	public void init() {
		injector = createInjector();
	}

	protected abstract Injector createInjector();

	@Test
	public void injectInstance() {
		Bar bar = new Bar();
		injector.injectMembers(bar);
		assertNotNull(bar.service);
	}

	@Test
	public void memberInjector() {
		Bar bar = new Bar();
		injector.getMembersInjector(Bar.class).injectMembers(bar);
		assertNotNull(bar.service);
	}

	@Test
	public void getInstanceUnbound() {
		assertNotNull(injector.getInstance(Foo.class));
	}

	@Test
	public void getInstanceBound() {
		assertNotNull(injector.getInstance(Service.class));
	}

	@Test
	public void getProviderUnbound() {
		assertNotNull(injector.getProvider(Foo.class).get());
	}

	@Test
	public void getProviderBound() {
		assertNotNull(injector.getProvider(Service.class).get());
	}

	interface Service {
	}

	protected static class MyService implements Service {
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

}
