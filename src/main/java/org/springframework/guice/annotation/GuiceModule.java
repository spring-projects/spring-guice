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

package org.springframework.guice.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.guice.module.GuiceModuleMetadata;
import org.springframework.guice.module.SpringModule;

/**
 * Annotation that decorates the whole application context and provides metadata to Guice
 * if the context is used as a Module. Can be added to any <code>@Configuration</code>
 * class (and if added to many then the filters are combined with logical OR). By default
 * all beans in the context will be bound to Guice with all of their implemented
 * interfaces. If you need to filter out which beans are added you can filter by class.
 * 
 * @author Dave Syer
 * 
 * @see SpringModule
 * @see GuiceModuleMetadata
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(GuiceModuleRegistrar.class)
public @interface GuiceModule {

	/**
	 * Specifies which types are eligible for inclusion in Guice module
	 */
	Filter[] includeFilters() default {};

	/**
	 * Specifies which types are not eligible for inclusion in Guice module.
	 */
	Filter[] excludeFilters() default {};

	/**
	 * Specifies which names (by regex) are eligible for inclusion in Guice module
	 */
	String[] includePatterns() default {};

	/**
	 * Specifies which bean names (by regex) are not eligible for inclusion in Guice module.
	 */
	String[] excludePatterns() default {};

	/**
	 * Specifies which names (by simple wildcard match) are eligible for inclusion in Guice module
	 */
	String[] includeNames() default {};

	/**
	 * Specifies which bean names (by simple wildcard match) are not eligible for inclusion in Guice module.
	 */
	String[] excludeNames() default {};

}
