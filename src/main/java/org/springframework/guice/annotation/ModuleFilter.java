package org.springframework.guice.annotation;

import com.google.inject.Module;

@FunctionalInterface
public interface ModuleFilter {

	boolean filter(Module module);

}
