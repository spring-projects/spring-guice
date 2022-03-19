/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.guice.annotation;

import javax.inject.Provider;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.inject.Injector;
import com.google.inject.Key;

/**
 * Convenience class used to map a Guice {@link Provider} to a Spring bean.
 *
 * @author Dave Syer
 */
class GuiceFactoryBean<T> implements FactoryBean<T> {

	private final Key<T> key;

	private final Class<T> beanType;

	private final boolean isSingleton;

	@Autowired
	private Injector injector;

	public GuiceFactoryBean(Class<T> beanType, Key<T> key, boolean isSingleton) {
		this.beanType = beanType;
		this.key = key;
		this.isSingleton = isSingleton;
	}

	@Override
	public T getObject() throws Exception {
		return (T) injector.getInstance(key);
	}

	@Override
	public Class<?> getObjectType() {
		return beanType;
	}

	@Override
	public boolean isSingleton() {
		return this.isSingleton;
	}

}