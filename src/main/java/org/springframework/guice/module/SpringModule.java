/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.guice.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.guice.injector.GuiceAutowireCandidateResolver;
import org.springframework.util.ClassUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Stage;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.google.inject.spi.ProvisionListener;

/**
 * @author Dave Syer
 *
 */
public class SpringModule extends AbstractModule {

	private BindingTypeMatcher matcher = new GuiceModuleMetadata();

	private Map<Class<?>, Provider<?>> bound = new HashMap<Class<?>, Provider<?>>();
	
    private final Class<?>[] configClasses;
    private final String[] basePackages;
    private ApplicationContext context;
    
    public SpringModule(Class<?>... configClasses) {
        this.configClasses = configClasses; 
        this.basePackages = null;
    }
    
    public SpringModule(String... basePackages) {
        this.basePackages = basePackages;
        this.configClasses = null;
    }
    
    public SpringModule(ApplicationContext context) {
        this.context = context;   
        this.configClasses = null;
        this.basePackages = null;
    }

	@Override
	public void configure() {
		if (binder().currentStage() != Stage.TOOL) {
			ConfigurableListableBeanFactory beanFactory = null;
			if (context == null) {
				final PartiallyRefreshableApplicationContext guiceAwareContext = new PartiallyRefreshableApplicationContext();
				if (basePackages != null) {
					guiceAwareContext.scan(basePackages);
				}
				if (configClasses != null) {
					guiceAwareContext.register(configClasses);
				}

				guiceAwareContext.getDefaultListableBeanFactory().setAutowireCandidateResolver(
						new GuiceAutowireCandidateResolver(binder().getProvider(Injector.class)));
				guiceAwareContext.partialRefresh();	//make sure all definitions are readable
				beanFactory = guiceAwareContext.getBeanFactory();

				binder().bindListener(Matchers.any(), new ContextRefreshingProvisionListener(guiceAwareContext));
			} else {
				beanFactory = (ConfigurableListableBeanFactory) context.getAutowireCapableBeanFactory();
			}

			if (beanFactory.getBeanNamesForType(GuiceModuleMetadata.class).length > 0) {
				this.matcher = new CompositeTypeMatcher(beanFactory.getBeansOfType(GuiceModuleMetadata.class).values());
			}

			bindDefinitionsForBeanFactory(beanFactory);
		}
	}

	protected void bindDefinitionsForBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		for (String name : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition definition = beanFactory.getBeanDefinition(name);
			if (definition.isAutowireCandidate() && definition.getRole() == AbstractBeanDefinition.ROLE_APPLICATION) {
				Class<?> type = beanFactory.getType(name);
				@SuppressWarnings("unchecked")
				final Class<Object> cls = (Class<Object>) type;
				final String beanName = name;
				Provider<Object> typeProvider = new BeanFactoryProvider(beanFactory, null, type);
				Provider<Object> namedProvider = new BeanFactoryProvider(beanFactory, beanName, type);
				if (!cls.isInterface() && !ClassUtils.isCglibProxyClass(cls)) {
					bindConditionally(binder(), name, cls, typeProvider, namedProvider);
				}
				for (Class<?> iface : ClassUtils.getAllInterfacesForClass(cls)) {
					@SuppressWarnings("unchecked")
					Class<Object> unchecked = (Class<Object>) iface;
					bindConditionally(binder(), name, unchecked, typeProvider, namedProvider);
				}
			}
		}
	}

	private void bindConditionally(Binder binder, String name, Class<Object> type, Provider<Object> typeProvider,
			Provider<Object> namedProvider) {
		if (!this.matcher.matches(name, type)) {
			return;
		}
		if (type.getName().startsWith("com.google.inject")) {
			return;
		}
		if (this.bound.get(type) == null) {
			// Only bind one provider for each type
			binder.withSource("spring-guice").bind(type).toProvider(typeProvider);
			this.bound.put(type, typeProvider);
		}
		// But allow binding to named beans
		binder.withSource("spring-guice").bind(type).annotatedWith(Names.named(name)).toProvider(namedProvider);
	}
	
	private final class PartiallyRefreshableApplicationContext extends AnnotationConfigApplicationContext {

		private final AtomicBoolean partiallyRefreshed = new AtomicBoolean(false);

		/*
		 * Initializes beanFactoryPostProcessors only to ensure that all
		 * BeanDefinition's are available
		 */
		void partialRefresh() {
			invokeBeanFactoryPostProcessors(getBeanFactory());
		}

		@Override
		protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
			if (partiallyRefreshed.compareAndSet(false, true)) {
				super.invokeBeanFactoryPostProcessors(beanFactory);
			}
		}
	}

	private final class ContextRefreshingProvisionListener implements ProvisionListener {
		private final ConfigurableApplicationContext context;
		private final AtomicBoolean initialized = new AtomicBoolean(false);

		private ContextRefreshingProvisionListener(ConfigurableApplicationContext context) {
			this.context = context;
		}

		@Override
		public <T> void onProvision(ProvisionInvocation<T> provision) {
			if (!initialized.getAndSet(true)) {
				context.refresh();
			}
			provision.provision();
		}
	}

	private static class BeanFactoryProvider implements Provider<Object> {

		private ConfigurableListableBeanFactory beanFactory;

		private String name;

		private Class<?> type;

		private Object result;

		public BeanFactoryProvider(ConfigurableListableBeanFactory beanFactory, String name, Class<?> type) {
			this.beanFactory = beanFactory;
			this.name = name;
			this.type = type;
		}

		@Override
		public Object get() {
			if (this.result == null) {

				String[] named = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory, this.type);
				List<String> names = new ArrayList<String>(named.length);
				if (named.length == 1) {
					names.add(named[0]);
				} else {
					for (String name : named) {
						if (name.equals(this.name))
							names.add(name);
					}
				}
				if (names.size() == 1) {
					this.result = this.beanFactory.getBean(names.get(0), this.type);
				} else {
					for (String name : named) {
						if (this.beanFactory.getBeanDefinition(name).isPrimary()) {
							this.result = this.beanFactory.getBean(name, this.type);
							break;
						}
					}
					if (this.result == null) {
						throw new ProvisionException("No primary bean definition for type: " + this.type);
					}
				}
			}
			return this.result;
		}
	}

	private static class CompositeTypeMatcher implements BindingTypeMatcher {
		private Collection<? extends BindingTypeMatcher> matchers;

		public CompositeTypeMatcher(Collection<? extends BindingTypeMatcher> matchers) {
			this.matchers = matchers;
		}

		@Override
		public boolean matches(String name, Class<?> type) {
			for (BindingTypeMatcher matcher : this.matchers) {
				if (matcher.matches(name, type)) {
					return true;
				}
			}
			return false;
		}
	}
}
