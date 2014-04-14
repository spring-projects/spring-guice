This project provides bridges between Spring and Guice so that you can
use one from the other (and vice versa)

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

## Using existing Guice Modules in a Spring ApplicationContext

The main feature here is a Spring `@Configuration` annotation:
`@EnableGuiceModules`. If you have Guice `Modules` that you want to
re-use (e.g. if they come from a third party) you can declare them in
a Spring `ApplicationContext` as `@Beans`, and expose all their
bindings. Example:

```java
@EnableGuiceModules
@Configuration
public static class TestConfig extends AbstractModule {

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

## Limitations

* So far there is no support for the Guice SPI methods in
  `SpringInjector` so tooling may not work. It wouldn't be hard to do.

* `SpringInjector` only knows about raw types, so it ignores
  additional meta-information in factory requests (like
  annotations). Should be easy enough to fix, but some compromises
  might hav eto be made.
  
* `SpringInjector` has no support for creating child or parent
  `Injectors`. Probably not difficult.

* `SpringModule` treats all beans as singletons.

* `SpringModule` binds all interfaces of a bean it can find. This will
  cause issues sooner rather than later (e.g. when 2 beans implement
  the same interface).
