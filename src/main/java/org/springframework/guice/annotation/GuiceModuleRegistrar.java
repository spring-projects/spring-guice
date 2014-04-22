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

package org.springframework.guice.annotation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AspectJTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.guice.module.GuiceModuleMetadata;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 *
 */
public class GuiceModuleRegistrar implements ImportBeanDefinitionRegistrar,
		ResourceLoaderAware {

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata annotation,
			BeanDefinitionRegistry registry) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(GuiceModuleMetadataFactory.class);
		builder.addPropertyValue("includeFilters",
				parseFilters(annotation, "includeFilters"));
		builder.addPropertyValue("excludeFilters",
				parseFilters(annotation, "excludeFilters"));
		builder.addPropertyValue("includePatterns",
				parsePatterns(annotation, "includePatterns"));
		builder.addPropertyValue("excludePatterns",
				parsePatterns(annotation, "excludePatterns"));
		builder.addPropertyValue("includeNames",
				parseNames(annotation, "includeNames"));
		builder.addPropertyValue("excludeNames",
				parseNames(annotation, "excludeNames"));
		AbstractBeanDefinition definition = builder.getBeanDefinition();
		String name = new DefaultBeanNameGenerator().generateBeanName(definition,
				registry);
		registry.registerBeanDefinition(name, definition);
	}

	public static class GuiceModuleMetadataFactory implements
			FactoryBean<GuiceModuleMetadata> {

		private Collection<? extends TypeFilter> includeFilters;

		private Collection<? extends TypeFilter> excludeFilters;

		private Collection<Pattern> includePatterns;

		private Collection<Pattern> excludePatterns;

		private Collection<String> includeNames;

		private Collection<String> excludeNames;

		public void setIncludeFilters(Collection<? extends TypeFilter> includeFilters) {
			this.includeFilters = includeFilters;
		}

		public void setExcludeFilters(Collection<? extends TypeFilter> excludeFilters) {
			this.excludeFilters = excludeFilters;
		}

		public void setIncludePatterns(Collection<Pattern> includePatterns) {
			this.includePatterns = includePatterns;
		}

		public void setExcludePatterns(Collection<Pattern> excludePatterns) {
			this.excludePatterns = excludePatterns;
		}

		public void setIncludeNames(Collection<String> includeNames) {
			this.includeNames = includeNames;
		}

		public void setExcludeNames(Collection<String> excludeNames) {
			this.excludeNames = excludeNames;
		}

		@Override
		public GuiceModuleMetadata getObject() throws Exception {
			return new GuiceModuleMetadata()
					.include(
							includeFilters.toArray(new TypeFilter[includeFilters.size()]))
					.exclude(
							excludeFilters.toArray(new TypeFilter[excludeFilters.size()]))
					.include(
							includePatterns.toArray(new Pattern[includePatterns.size()]))
					.exclude(
							excludePatterns.toArray(new Pattern[excludePatterns.size()]))
					.include(
							includeNames.toArray(new String[includeNames.size()]))
					.exclude(
							excludeNames.toArray(new Pattern[excludeNames.size()]));
		}

		@Override
		public Class<?> getObjectType() {
			return GuiceModuleMetadata.class;
		}

		@Override
		public boolean isSingleton() {
			return false;
		}

	}

	private Set<Pattern> parsePatterns(AnnotationMetadata annotation, String attributeName) {
		Set<Pattern> result = new HashSet<Pattern>();
		AnnotationAttributes attributes = new AnnotationAttributes(
				annotation.getAnnotationAttributes(GuiceModule.class.getName()));
		String[] filters = attributes.getStringArray(attributeName);

		for (String filter : filters) {
			result.add(Pattern.compile(filter));
		}

		return result;
	}

	private Set<String> parseNames(AnnotationMetadata annotation, String attributeName) {
		Set<String> result = new HashSet<String>();
		AnnotationAttributes attributes = new AnnotationAttributes(
				annotation.getAnnotationAttributes(GuiceModule.class.getName()));
		String[] filters = attributes.getStringArray(attributeName);

		for (String filter : filters) {
			result.add(filter);
		}

		return result;
	}

	private Set<TypeFilter> parseFilters(AnnotationMetadata annotation,
			String attributeName) {

		Set<TypeFilter> result = new HashSet<TypeFilter>();
		AnnotationAttributes attributes = new AnnotationAttributes(
				annotation.getAnnotationAttributes(GuiceModule.class.getName()));
		AnnotationAttributes[] filters = attributes.getAnnotationArray(attributeName);

		for (AnnotationAttributes filter : filters) {
			result.addAll(typeFiltersFor(filter));
		}

		return result;
	}

	private List<TypeFilter> typeFiltersFor(AnnotationAttributes filterAttributes) {

		List<TypeFilter> typeFilters = new ArrayList<TypeFilter>();
		FilterType filterType = filterAttributes.getEnum("type");

		for (Class<?> filterClass : filterAttributes.getClassArray("value")) {
			switch (filterType) {
			case ANNOTATION:
				Assert.isAssignable(Annotation.class, filterClass,
						"An error occured when processing a @ComponentScan "
								+ "ANNOTATION type filter: ");
				@SuppressWarnings("unchecked")
				Class<Annotation> annoClass = (Class<Annotation>) filterClass;
				typeFilters.add(new AnnotationTypeFilter(annoClass));
				break;
			case ASSIGNABLE_TYPE:
				typeFilters.add(new AssignableTypeFilter(filterClass));
				break;
			case CUSTOM:
				Assert.isAssignable(TypeFilter.class, filterClass,
						"An error occured when processing a @ComponentScan "
								+ "CUSTOM type filter: ");
				typeFilters
						.add(BeanUtils.instantiateClass(filterClass, TypeFilter.class));
				break;
			default:
				throw new IllegalArgumentException("Unknown filter type " + filterType);
			}
		}

		for (String expression : getPatterns(filterAttributes)) {

			String rawName = filterType.toString();

			if ("REGEX".equals(rawName)) {
				typeFilters.add(new RegexPatternTypeFilter(Pattern.compile(expression)));
			} else if ("ASPECTJ".equals(rawName)) {
				typeFilters.add(new AspectJTypeFilter(expression, this.resourceLoader
						.getClassLoader()));
			} else {
				throw new IllegalArgumentException("Unknown filter type " + filterType);
			}
		}

		return typeFilters;
	}

	private String[] getPatterns(AnnotationAttributes filterAttributes) {

		try {
			return filterAttributes.getStringArray("pattern");
		} catch (IllegalArgumentException o_O) {
			return new String[0];
		}
	}

}
