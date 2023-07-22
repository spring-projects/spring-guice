/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.guice.annotation;

import java.util.function.Predicate;

import com.google.inject.Module;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * A filter to remove {@link Module}:s from the context before any initialization code is
 * run. If one implementation of this interface returns false from its
 * {@link #test(Object)} method, the {@link Module} is removed. See
 * {@link ModuleRegistryConfiguration#postProcessBeanDefinitionRegistry(BeanDefinitionRegistry)}
 *
 * @author Niklas Herder
 *
 */

public interface ModuleFilter extends Predicate<Module> {

}
