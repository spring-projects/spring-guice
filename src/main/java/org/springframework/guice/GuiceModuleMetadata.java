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

package org.springframework.guice;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Encapsulates some metadata about a Guice module that is to be created from the parent
 * context of a <code>@Bean</code> of this type.
 * 
 * @author Dave Syer
 *
 */
public class GuiceModuleMetadata {

	private TypeFilter[] includeFilters;

	private TypeFilter[] excludeFilters;

	private Set<Class<?>> infrastructureTypes = new HashSet<Class<?>>();

	{
		infrastructureTypes.add(InitializingBean.class);
		infrastructureTypes.add(DisposableBean.class);
	}

	private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

	public GuiceModuleMetadata include(TypeFilter... filters) {
		includeFilters = filters;
		return this;
	}

	public GuiceModuleMetadata exclude(TypeFilter... filters) {
		excludeFilters = filters;
		return this;
	}

	public boolean matches(Class<?> type) {

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
			if (!Modifier.isPublic(cls.getModifiers())) {
				return false;
			}
			cls = cls.getDeclaringClass();
		}
		return true;
	}

}
