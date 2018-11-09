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
package org.springframework.guice.module;

import javax.inject.Provider;

import com.google.inject.Key;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.util.Assert;

import com.google.inject.Injector;

/**
 * @author Dave Syer
 * @author Taylor Wicksell
 * @author Howard Yuan
 *
 */
class GuiceAutowireCandidateResolver extends ContextAnnotationAutowireCandidateResolver {
    
    private Provider<Injector> injectorProvider;

    public GuiceAutowireCandidateResolver(Provider<Injector> injectorProvider) {
        this.injectorProvider = injectorProvider;
    }

    @Override
    public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, String beanName) {
        return (isLazy(descriptor, beanName) ? buildLazyResolutionProxy(descriptor, beanName) : null);
    }

    protected boolean isLazy(DependencyDescriptor descriptor, String beanName) {
        Assert.state(getBeanFactory() instanceof DefaultListableBeanFactory,
                "BeanFactory needs to be a DefaultListableBeanFactory");
        final DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) getBeanFactory();
        try {
            beanFactory.doResolveDependency(descriptor, beanName, null, null);
        } catch (NoSuchBeanDefinitionException e) {
            return true;
        }
        return super.isLazy(descriptor);
    }

    protected Object buildLazyResolutionProxy(final DependencyDescriptor descriptor, final String beanName) {
        Assert.state(getBeanFactory() instanceof DefaultListableBeanFactory,
                "BeanFactory needs to be a DefaultListableBeanFactory");
        final DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) getBeanFactory();
        TargetSource ts = new TargetSource() {
            @Override
            public Class<?> getTargetClass() {
                return descriptor.getDependencyType();
            }

            @Override
            public boolean isStatic() {
                return false;
            }

            @Override
            public Object getTarget() {
                Object target = null;
                try {
                    target = beanFactory.doResolveDependency(descriptor, beanName, null, null);
                } catch (NoSuchBeanDefinitionException e) {
                    target = injectorProvider.get().getInstance(Key.get(descriptor.getResolvableType().getType()));
                }
                if (target == null) {
                     throw new NoSuchBeanDefinitionException(descriptor.getDependencyType(),
                            "Optional dependency not present for lazy injection point");
                }
                return target;
            }

            @Override
            public void releaseTarget(Object target) {
            }
        };
        ProxyFactory pf = new ProxyFactory();
        pf.setTargetSource(ts);
        Class<?> dependencyType = descriptor.getDependencyType();
        if (dependencyType.isInterface()) {
            pf.addInterface(dependencyType);
        }
        return pf.getProxy(beanFactory.getBeanClassLoader());
    }

}
