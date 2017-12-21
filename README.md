This project provides bridges between Spring and Guice so that you can
use one from the other (and vice versa)

![Build Status](https://travis-ci.org/spring-projects/spring-guice.svg?branch=master)

## Using a Spring ApplicationContext as a Module in Guice

The main bridge in this case is a Guice `Module` that wraps an
existing Spring `ApplicationContext`. Example:

```java
AnnotationConfigApplicationContext context = 
    new AnnotationConfigApplicationContext(ApplicationConfiguration.class);
Injector injector = Guice.createInjector(new SpringModule(context), new MyModule());
Service service = injector.getInstance(Service.class);
```

Note that the `ApplicationContext` in this example might contain the
`Service` definition or it might be in the Guice `Module`
(`MyModule`), or if `Service` is a concrete class it could be neither,
but Guice creates an instance and wires it for us.

If the `ApplicationConfiguration` is annotated `@GuiceModule` then it
can filter the types of bean that are registered with the Guice
binder. Example:

```java
@Configuration
@GuiceModule(includeFilters=@Filter(pattern=.*\\.Service))
public class ApplicationConfiguration {
    @Bean
    public MyService service() {
        ...
    }
}
```

In this case, only bean types (or interfaces) called "Service" will
match the include filter, and only those beans will be bound.

If there are multiple `@Beans` of the same type in the
`ApplicationContext` then the `SpringModule` will register them all,
and there will be a runtime exception if an `Injector` needs one. As
with normal Spring dependency resolution, you can add the `@Primary`
marker to a single bean to differentiate and hint to the `Injector`
which instance to use.

## Registering Spring Configuration Classes as a Guice Module

If your Spring `@Configuration` has dependencies that can only come
from a Guice `Module` and you prefer to use the Guice APIs to build up
the configuration (so you can't use `@EnableGuiceModules` below), then
you can create a `SpringModule` from a
`Provider<ConfigurableListableBeanFactory>` instead of from an
existing `ApplicationContext`. There are some additional features that
may also apply:

* If the bean factory created by the provider is a
`DefaultListableBeanFactory` (mostly it would be if it came from an
`ApplicationContext`), then it will pick up a special Guice-aware
`AutowireCandidateResolver`, meaning that it will be able to inject
dependencies from Guice modules that are not registered as beans.

* If the bean factory contains any beans of type `ProvisionListener`
(a Guice lifecysle listener), then those will be instantiated and
registered with Guice.

To take advantage of the autowiring the bean factory must come from an
`ApplicationContext` that is not fully refreshed (refreshing would
resolve all the dependencies and fail because the Guice resolver is
not yet registered). To help you build bean factories that have this
quality there is a convenience class called `BeanFactoryProvider` with
static methods which you can use to create a provider to inject into a
`SpringModule`.  Example:

```java
Injector injector = Guice.createInjector(new SimpleGuiceModule(), 
    new SpringModule(BeanFactoryProvider.from(SpringConfiguration.class)));
```

The `SimpleGuiceModule` contains a component that the
`SpringConfiguration` depends on.

## Using existing Guice Modules in a Spring ApplicationContext

The main feature here is a Spring `@Configuration` annotation:
`@EnableGuiceModules`. If you have Guice `Modules` that you want to
re-use (e.g. if they come from a third party) you can declare them in
a Spring `ApplicationContext` as `@Beans`, and expose all their
bindings. Example:

```java
@EnableGuiceModules
@Configuration
public static class TestConfig {

    @Bean
    public MyModule myModule() {
        return new MyModule();
    }

    @Bean
    public Spam spam(Service service) {
        return new Spam(service);
    }

}
```

The `Service` was defined in the Guice module `MyModule`, and then it
was be bound to the autowired `spam()` method when Spring started.

## Using Guice as an API for accessing a Spring ApplicationContext

In this case the main feature is an `Injector` implementation that
wraps a Spring `ApplicationContext`. Example:

```java
AnnotationConfigApplicationContext context = 
    new AnnotationConfigApplicationContext(ApplicationConfiguration.class);
Injector injector = new SpringInjector(context);
Service service = injector.getInstance(Service.class);
```

If there is a `@Bean` of type `Service` it will be returned from the
`Injector`. But there may actually not be a `@Bean` definition of type
`Service`, and if it is a concrete type then the `Injector` will
create it and autowire its dependencies for you. A side effect of this
is that a `BeanDefinition` *will* be created.

In the example above, if `ApplicationConfiguration` was annotated
`@EnableGuiceModules` then there is an `Injector` bean already waiting
to be used, including wiring it into the application itself if you
need it. So this works as well:

```java
@Configuration
@EnableGuiceModules
public class ApplicationConfiguration {

    @Autowired
    private Injector injector;
    
    @Bean
    public Foo foo() {
        // Guice creates and does the wiring of Foo instead of Spring
        return injector.getInstance(Foo.class);
    }
}
```

In this example if the `Injector` has a binding for a `Provider` of
`Foo.class` then the `foo()` method is redundant - it is already
resolvable as a Spring dependency. But if Guice is being used as a
factory for new objects that it doesn't have bindings for, then it
makes sense.

**Note:** if you also have `@GuiceModule` in your context, then using
the injector to create a `@Bean` directly is a bad idea (there's a
dependency cycle). You *can* do it, and break the cycle, if you
exclude the `@Bean` type from the `Injector` bindings using the
`@GuiceModule` exclude filters.

## Limitations

* So far there is no support for the Guice SPI methods in
  `SpringInjector` so tooling may not work. It wouldn't be hard to do.

* `SpringInjector` only knows about raw types and bean names, so it
  ignores additional meta-information in factory requests (like
  annotations other than `@Named`). Should be easy enough to fix, but
  some compromises might have to be made.
  
* `SpringInjector` has no support for creating child or parent
  `Injectors`. Probably not difficult.

* `SpringModule` treats all beans as singletons.

* `SpringModule` binds all interfaces and all names of a bean it can
  find. This should work out OK, as long as those interfaces are not
  needed for injection (and if there is no `@Primary` bean).
