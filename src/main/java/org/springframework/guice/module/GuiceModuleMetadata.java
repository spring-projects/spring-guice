/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.guice.module;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.guice.annotation.GuiceModule;
import org.springframework.util.PatternMatchUtils;

/**
 * Encapsulates some metadata about a Guice module that is to be created from a Spring
 * application context. Can be used directly as a <code>@Bean</code>, but it is easier to
 * just add <code>@</code> {@link GuiceModule} to your <code>@Configuration</code>.
 *
 * @author Dave Syer
 *
 */
public class GuiceModuleMetadata implements BindingTypeMatcher {

	private TypeFilter[] includeFilters;

	private TypeFilter[] excludeFilters;

	private Pattern[] includePatterns;

	private Pattern[] excludePatterns;

	private String[] includeNames;

	private String[] excludeNames;

	private Set<Class<?>> infrastructureTypes = new HashSet<Class<?>>();

	{
		this.infrastructureTypes.add(InitializingBean.class);
		this.infrastructureTypes.add(DisposableBean.class);
	}

	private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

	public GuiceModuleMetadata include(String... filters) {
		this.includeNames = filters;
		return this;
	}

	public GuiceModuleMetadata exclude(String... filters) {
		this.excludeNames = filters;
		return this;
	}

	public GuiceModuleMetadata include(Pattern... filters) {
		this.includePatterns = filters;
		return this;
	}

	public GuiceModuleMetadata exclude(Pattern... filters) {
		this.excludePatterns = filters;
		return this;
	}

	public GuiceModuleMetadata include(TypeFilter... filters) {
		this.includeFilters = filters;
		return this;
	}

	public GuiceModuleMetadata exclude(TypeFilter... filters) {
		this.excludeFilters = filters;
		return this;
	}

	@Override
	public boolean matches(String name, Type type) {
		Type rawType = (type instanceof ParameterizedType) ? ((ParameterizedType) type).getRawType() : type;
		return matches(name) && matches(rawType);
	}

	private boolean matches(String name) {
		if (this.includePatterns != null) {
			for (Pattern filter : this.includePatterns) {
				if (!filter.matcher(name).matches()) {
					return false;
				}
			}
		}
		if (this.excludePatterns != null) {
			for (Pattern filter : this.excludePatterns) {
				if (filter.matcher(name).matches()) {
					return false;
				}
			}
		}
		if (this.includeNames != null && this.includeNames.length > 0) {
			if (!PatternMatchUtils.simpleMatch(this.includeNames, name)) {
				return false;
			}
		}
		if (this.excludeNames != null && this.excludeNames.length > 0) {
			return !PatternMatchUtils.simpleMatch(this.excludeNames, name);
		}
		return true;
	}

	private boolean matches(Type type) {
		if (this.infrastructureTypes.contains(type)) {
			return false;
		}

		if (!visible(type)) {
			return false;
		}

		if (this.includeFilters != null) {
			try {
				MetadataReader reader = this.metadataReaderFactory.getMetadataReader(type.getTypeName());
				for (TypeFilter filter : this.includeFilters) {
					if (!filter.match(reader, this.metadataReaderFactory)) {
						return false;
					}
				}
			}
			catch (IOException ex) {
				throw new IllegalStateException("Cannot read metadata for class " + type, ex);
			}
		}
		if (this.excludeFilters != null) {
			try {
				MetadataReader reader = this.metadataReaderFactory.getMetadataReader(type.getTypeName());
				for (TypeFilter filter : this.excludeFilters) {
					if (filter.match(reader, this.metadataReaderFactory)) {
						return false;
					}
				}
			}
			catch (IOException ex) {
				throw new IllegalStateException("Cannot read metadata for class " + type, ex);
			}
		}
		return true;
	}

	private boolean visible(Type type) {
		Class<?> cls = ResolvableType.forType(type).resolve();
		while (cls != null && cls != Object.class) {
			if (!Modifier.isInterface(cls.getModifiers()) && !Modifier.isPublic(cls.getModifiers())
					&& !Modifier.isProtected(cls.getModifiers())) {
				return false;
			}
			cls = cls.getDeclaringClass();
		}
		return true;
	}

}
