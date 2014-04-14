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
	public void getInstanceUnbound() {
		assertNotNull(injector.getInstance(Foo.class));
	}

	@Test
	public void getInstanceBound() {
		assertNotNull(injector.getInstance(Service.class));
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

}
