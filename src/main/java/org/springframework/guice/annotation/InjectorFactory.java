package org.springframework.guice.annotation;

import java.util.List;

import com.google.inject.Injector;
import com.google.inject.Module;

/***
 * Factory which allows for custom creation of the Guice Injector to be used in
 * the @{@link EnableGuiceModules} feature.
 */
public interface InjectorFactory {

	public Injector createInjector(List<Module> modules);

}
