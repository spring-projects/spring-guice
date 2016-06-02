/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.guice.injector;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.AutowireCandidateResolver;

/**
 * @author Dave Syer
 *
 */
public class CompositeAutowireCandidateResolver implements AutowireCandidateResolver {

	private List<AutowireCandidateResolver> delegates;

	public CompositeAutowireCandidateResolver(List<AutowireCandidateResolver> delegates) {
		this.delegates = delegates;
	}

	@Override
	public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder,
			DependencyDescriptor descriptor) {
		for (AutowireCandidateResolver delegate : this.delegates) {
			if (delegate.isAutowireCandidate(bdHolder, descriptor)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Object getSuggestedValue(DependencyDescriptor descriptor) {
		for (AutowireCandidateResolver delegate : this.delegates) {
			Object value = delegate.getSuggestedValue(descriptor);
			if (value!=null) {
				return value;
			}
		}
		return null;
	}

	@Override
	public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor,
			String beanName) {
		for (AutowireCandidateResolver delegate : this.delegates) {
			Object value = delegate.getLazyResolutionProxyIfNecessary(descriptor, beanName);
			if (value!=null) {
				return value;
			}
		}
		return null;
	}

}
