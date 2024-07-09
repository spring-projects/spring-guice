/*
 * Copyright 2018-2022 the original author or authors.
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
import com.google.inject.Module;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

import static org.assertj.core.api.Assertions.assertThat;

public class BeanPostProcessorTests {

	/**
	 * Verify BeanPostProcessor's such as Spring Boot's
	 * ConfigurationPropertiesBindingPostProcessor are applied.
	 */
	@Test
	public void testBeanPostProcessorsApplied() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BeanPostProcessorTestConfig.class);
		PostProcessedBean postProcessedBean = context.getBean(PostProcessedBean.class);
		assertThat(postProcessedBean.postProcessed).isTrue();
		GuiceBeanThatWantsPostProcessedBean guiceBean1 = context.getBean(GuiceBeanThatWantsPostProcessedBean.class);
		assertThat(guiceBean1.ppb.postProcessed).isTrue();
		GuiceBeanThatWantsSpringBean guiceBean2 = context.getBean(GuiceBeanThatWantsSpringBean.class);
		assertThat(guiceBean2.springBean.ppb.postProcessed).isTrue();
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

	@EnableGuiceModules
	@Configuration
	static class BeanPostProcessorTestConfig {

		@Bean
		static PostProcessorRegistrar postProcessorRegistrar() {
			return new PostProcessorRegistrar();
		}

		@Bean
		PostProcessedBean postProcessedBean() {
			return new PostProcessedBean();
		}

		@Bean
		SpringBeanThatWantsPostProcessedBean springBean(PostProcessedBean ppb) {
			return new SpringBeanThatWantsPostProcessedBean(ppb);
		}

		@Bean
		static Module someGuiceModule() {
			return new AbstractModule() {

				@Override
				protected void configure() {
					binder().requireExplicitBindings();
					bind(GuiceBeanThatWantsPostProcessedBean.class).asEagerSingleton();
					bind(GuiceBeanThatWantsSpringBean.class).asEagerSingleton();
				}
			};
		}

		public static class PostProcessorRegistrar implements BeanDefinitionRegistryPostProcessor {

			@Override
			public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
				BeanDefinitionBuilder bean = BeanDefinitionBuilder.genericBeanDefinition(TestBeanPostProcessor.class);
				registry.registerBeanDefinition("postProcessor", bean.getBeanDefinition());
			}

			@Override
			public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			}

		}

		public static class TestBeanPostProcessor implements BeanPostProcessor {

			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
				if (bean instanceof PostProcessedBean) {
					((PostProcessedBean) bean).postProcessed = true;
				}
				return bean;
			}

			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				return bean;
			}

		}

	}

}
