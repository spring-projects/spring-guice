package org.springframework.guice.annotation;

import javax.inject.Provider;

import org.springframework.beans.factory.FactoryBean;

public class GuiceFactoryBean<T> implements FactoryBean<T> {
		private final Provider<T> provider;
		private final Class<T> beanType;
		
		public GuiceFactoryBean(Class<T> beanType, Provider<T> provider) {
			this.provider = provider;
			this.beanType = beanType;
		}

		@Override
		public T getObject() throws Exception {
			return (T) provider.get();
		}

		@Override
		public Class<?> getObjectType() {
			return beanType;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}