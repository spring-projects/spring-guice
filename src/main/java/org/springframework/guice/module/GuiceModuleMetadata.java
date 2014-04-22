/*
 * Copyright 2012-2013 the original author or authors.
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

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
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
		infrastructureTypes.add(InitializingBean.class);
		infrastructureTypes.add(DisposableBean.class);
	}

	private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

	public GuiceModuleMetadata include(String... filters) {
		includeNames = filters;
		return this;
	}

	public GuiceModuleMetadata exclude(String... filters) {
		excludeNames = filters;
		return this;
	}

	public GuiceModuleMetadata include(Pattern... filters) {
		includePatterns = filters;
		return this;
	}

	public GuiceModuleMetadata exclude(Pattern... filters) {
		excludePatterns = filters;
		return this;
	}

	public GuiceModuleMetadata include(TypeFilter... filters) {
		includeFilters = filters;
		return this;
	}

	public GuiceModuleMetadata exclude(TypeFilter... filters) {
		excludeFilters = filters;
		return this;
	}

	@Override
	public boolean matches(String name, Class<?> type) {
		if (!matches(name) || !matches(type)) {
			return false;
		}
		return true;
	}

	private boolean matches(String name) {
		if (includePatterns != null) {
			for (Pattern filter : includePatterns) {
				if (!filter.matcher(name).matches()) {
					return false;
				}
			}
		}
		if (excludePatterns != null) {
			for (Pattern filter : excludePatterns) {
				if (filter.matcher(name).matches()) {
					return false;
				}
			}
		}
		if (includeNames != null && includeNames.length > 0) {
			if (!PatternMatchUtils.simpleMatch(includeNames, name)) {
				return false;
			}
		}
		if (excludeNames != null && excludeNames.length > 0) {
			if (PatternMatchUtils.simpleMatch(excludeNames, name)) {
				return false;
			}
		}
		return true;
	}

	private boolean matches(Class<?> type) {
		if (infrastructureTypes.contains(type)) {
			return false;
		}

		if (!visible(type)) {
			return false;
		}

		if (includeFilters != null) {
			try {
				MetadataReader reader = metadataReaderFactory.getMetadataReader(type
						.getName());
				for (TypeFilter filter : includeFilters) {
					if (!filter.match(reader, metadataReaderFactory)) {
						return false;
					}
				}
			} catch (IOException e) {
				throw new IllegalStateException("Cannot read metadata for class " + type,
						e);
			}
		}
		if (excludeFilters != null) {
			try {
				MetadataReader reader = metadataReaderFactory.getMetadataReader(type
						.getName());
				for (TypeFilter filter : excludeFilters) {
					if (filter.match(reader, metadataReaderFactory)) {
						return false;
					}
				}
			} catch (IOException e) {
				throw new IllegalStateException("Cannot read metadata for class " + type,
						e);
			}
		}
		return true;
	}

	private boolean visible(Class<?> type) {
		Class<?> cls = type;
		while (cls != null && cls != Object.class) {
			if (!Modifier.isInterface(cls.getModifiers())
					&& !Modifier.isPublic(cls.getModifiers())
					&& !Modifier.isProtected(cls.getModifiers())) {
				return false;
			}
			cls = cls.getDeclaringClass();
		}
		return true;
	}

}
