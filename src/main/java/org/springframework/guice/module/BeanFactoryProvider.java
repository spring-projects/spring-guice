/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.guice.module;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Provider;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.OrderComparator;

import com.google.inject.spi.ProvisionListener;

/**
 * <p>
 * A {@link Provider} for a {@link BeanFactory} from an
 * {@link ApplicationContext} that will not be refreshed until the Guice
 * injector wants to resolve dependencies. Delaying the refresh means that the
 * bean factory can resolve dependencies from Guice modules (and vice versa).
 * </p>
 * <p>
 * Also implements {@link Closeable} so if you want to clean up resources used
 * in the application context then you can keep a reference to the provider and
 * call {@link #close()} on it when the application is shut down. Alternatively,
 * you could register an {@link ApplicationContextInitializer} that sets a
 * shutdown hook, so that the context is closed automatically when the JVM ends.
 * </p>
 * 
 * @author Dave Syer
 *
 */
public class BeanFactoryProvider implements Provider<ConfigurableListableBeanFactory>, Closeable {

	private Class<?>[] config;
	private String[] basePackages;
	private List<ApplicationContextInitializer<ConfigurableApplicationContext>> initializers = new ArrayList<ApplicationContextInitializer<ConfigurableApplicationContext>>();
	private PartiallyRefreshableApplicationContext context;

	/**
	 * Create an application context by scanning these base packages.
	 * 
	 * @param basePackages base packages to scan
	 * @return a provider
	 */
	public static BeanFactoryProvider from(String... basePackages) {
		return new BeanFactoryProvider(null, basePackages);
	}

	/**
	 * Create an application context using these configuration classes.
	 * 
	 * @param config classes to build an application
	 * @return a provider
	 */
	public static BeanFactoryProvider from(Class<?>... config) {
		return new BeanFactoryProvider(config, null);
	}

	public BeanFactoryProvider initializer(
			ApplicationContextInitializer<ConfigurableApplicationContext>... initializers) {
		this.initializers.addAll(Arrays.asList(initializers));
		return this;
	}

	private BeanFactoryProvider(Class<?>[] config, String[] basePackages) {
		this.config = config;
		this.basePackages = basePackages;
	}

	@Override
	public void close() throws IOException {
		if (this.context != null) {
			synchronized (this) {
				if (this.context != null) {
					this.context.close();
					this.context = null;
				}
			}
		}
	}

	@Override
	public ConfigurableListableBeanFactory get() {
		if (this.context == null) {
			synchronized (this) {
				if (this.context == null) {
					PartiallyRefreshableApplicationContext context = new PartiallyRefreshableApplicationContext();
					if (config != null && config.length > 0) {
						context.register(config);
					}
					if (basePackages != null && basePackages.length > 0) {
						context.scan(basePackages);
					}
					context.partialRefresh();
					if (initializers != null && !initializers.isEmpty()) {
						OrderComparator.sort(initializers);
						for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : initializers) {
							initializer.initialize(context);
						}
					}
					this.context = context;
				}
			}
		}
		return context.getBeanFactory();
	}

	private static final class PartiallyRefreshableApplicationContext extends AnnotationConfigApplicationContext {

		private final AtomicBoolean partiallyRefreshed = new AtomicBoolean(false);

		/*
		 * Initializes beanFactoryPostProcessors only to ensure that all
		 * BeanDefinition's are available
		 */
		private void partialRefresh() {
			getBeanFactory().registerSingleton("refreshListener", new ContextRefreshingProvisionListener(this));
			invokeBeanFactoryPostProcessors(getBeanFactory());
		}

		private void delayedRefresh() throws BeansException, IllegalStateException {
			super.refresh();
		}

		@Override
		public void refresh() {
		}

		@Override
		protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
			if (partiallyRefreshed.compareAndSet(false, true)) {
				super.invokeBeanFactoryPostProcessors(beanFactory);
			}
		}
	}

	private static final class ContextRefreshingProvisionListener implements ProvisionListener {
		private final PartiallyRefreshableApplicationContext context;
		private final AtomicBoolean initialized = new AtomicBoolean(false);

		private ContextRefreshingProvisionListener(PartiallyRefreshableApplicationContext context) {
			this.context = context;
		}

		@Override
		public <T> void onProvision(ProvisionInvocation<T> provision) {
			if (!initialized.getAndSet(true) && !context.isActive()) {
				context.delayedRefresh();
			}
			provision.provision();
		}
	}

}
