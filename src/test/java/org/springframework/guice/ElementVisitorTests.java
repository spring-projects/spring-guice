package org.springframework.guice;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.ElementVisitorTests.DuplicateBean;
import org.springframework.guice.ElementVisitorTests.ElementVisitorTestGuiceBean;
import org.springframework.guice.ElementVisitorTests.ElementVisitorTestSpringBean;
import org.springframework.guice.annotation.EnableGuiceModules;
import org.springframework.guice.annotation.InjectorFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;

public class ElementVisitorTests {

	private static AnnotationConfigApplicationContext context;
	
	@BeforeClass
	public static void init() {
		context = new AnnotationConfigApplicationContext(ElementVisitorTestConfig.class);
	}
	
	@AfterClass
	public static void cleanup() {
		if(context != null) {
			context.close();
		}
	}

	@Test
	public void verifySpringModuleDoesNotBreakWhenUsingElementVisitors() {
		ElementVisitorTestSpringBean testSpringBean = context.getBean(ElementVisitorTestSpringBean.class);
		assertEquals("spring created", testSpringBean.toString());
		ElementVisitorTestGuiceBean testGuiceBean = context.getBean(ElementVisitorTestGuiceBean.class);
		assertEquals("spring created", testGuiceBean.toString());
	}


	public static class ElementVisitorTestSpringBean {
		@Override
		public String toString() {
			return "default";
		}
	}
	
	public static class ElementVisitorTestGuiceBean {
		@Inject
		ElementVisitorTestSpringBean springBean;
		@Override
		public String toString() {
			return springBean.toString();
		}
	}

	public static class DuplicateBean {}
}

@EnableGuiceModules
@Configuration
class ElementVisitorTestConfig {

	@Bean
	public ElementVisitorTestSpringBean testBean() {
		return new ElementVisitorTestSpringBean(){
			@Override
			public String toString() {
				return "spring created";
			}
		};
	}
	
	@Bean
	public Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				binder().requireExplicitBindings();
				bind(ElementVisitorTestGuiceBean.class).asEagerSingleton();
			}
		};
	}
	
	@Bean
	public InjectorFactory injectorFactory() {
		return new InjectorFactory() {	
			@Override
			public Injector createInjector(List<Module> modules) {
				List<Element> elements = Elements.getElements(Stage.TOOL, modules);
				return Guice.createInjector(Stage.PRODUCTION,Elements.getModule(elements));
			}
		};
	}
	
	@Bean
	public DuplicateBean dupeBean1() {
		return new DuplicateBean();
	}
	
	@Bean
	public DuplicateBean dupeBean2() {
		return new DuplicateBean();
	}
}