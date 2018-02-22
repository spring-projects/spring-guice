package org.springframework.guice;

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.BeanPostProcessorTest.GuiceBeanThatWantsPostProcessedBean;
import org.springframework.guice.BeanPostProcessorTest.GuiceBeanThatWantsSpringBean;
import org.springframework.guice.BeanPostProcessorTest.PostProcessedBean;
import org.springframework.guice.BeanPostProcessorTest.SpringBeanThatWantsPostProcessedBean;
import org.springframework.guice.annotation.EnableGuiceModules;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public class BeanPostProcessorTest {

	/**
	 * Verify BeanPostProcessor's such as Spring Boot's
	 * ConfigurationPropertiesBindingPostProcessor are applied.
	 */
	@Test
	public void testBeanPostProcessorsApplied() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(BeanPostProcessorTestConfig.class);
		PostProcessedBean postProcessedBean = context.getBean(PostProcessedBean.class);
		assertTrue(postProcessedBean.postProcessed);
		GuiceBeanThatWantsPostProcessedBean guiceBean1 = context.getBean(GuiceBeanThatWantsPostProcessedBean.class);
		assertTrue(guiceBean1.ppb.postProcessed);
		GuiceBeanThatWantsSpringBean guiceBean2 = context.getBean(GuiceBeanThatWantsSpringBean.class);
		assertTrue(guiceBean2.springBean.ppb.postProcessed);
		context.close();
	}
	
	public static class PostProcessedBean {
		Boolean postProcessed = false;
	}

	public static class SpringBeanThatWantsPostProcessedBean {
		PostProcessedBean ppb;
		public SpringBeanThatWantsPostProcessedBean(PostProcessedBean ppb) {
			this.ppb = ppb;
		}
	}

	public static class GuiceBeanThatWantsPostProcessedBean {
		PostProcessedBean ppb;
		@Inject
		public GuiceBeanThatWantsPostProcessedBean(PostProcessedBean ppb) {
			this.ppb = ppb;
		}
	}

	public static class GuiceBeanThatWantsSpringBean {
		SpringBeanThatWantsPostProcessedBean springBean;
		@Inject
		public GuiceBeanThatWantsSpringBean(SpringBeanThatWantsPostProcessedBean springBean) {
			this.springBean = springBean;
		}
	}
}


@EnableGuiceModules
@Configuration
class BeanPostProcessorTestConfig {
	@Bean
	public BeanPostProcessor postProcessor() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
				if(bean instanceof PostProcessedBean) {
					((PostProcessedBean)bean).postProcessed = true;
				}
				return bean;
			}
			
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				return bean;
			}
		};
	}
	
	@Bean
	public PostProcessedBean postProcessedBean() {
		return new PostProcessedBean();
	}
	
	@Bean
	public SpringBeanThatWantsPostProcessedBean springBean(PostProcessedBean ppb) {
		return new SpringBeanThatWantsPostProcessedBean(ppb);
	}
	
	@Bean
	public Module someGuiceModule() {
		return new AbstractModule() {
			
			@Override
			protected void configure() {
				binder().requireExplicitBindings();
				bind(GuiceBeanThatWantsPostProcessedBean.class).asEagerSingleton();
				bind(GuiceBeanThatWantsSpringBean.class).asEagerSingleton();
			}
		};
	}
}
