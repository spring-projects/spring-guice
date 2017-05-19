package org.springframework.guice.injector;

import java.util.List;

import org.springframework.guice.annotation.EnableGuiceModules;

import com.google.inject.Injector;
import com.google.inject.Module;

/***
 * Factory which allows for custom creation of the Guice Injector to be used
 * in the @{@link EnableGuiceModules} feature.
 */
public interface InjectorFactory {
    
    public Injector createInjector(List<Module> modules);

}
