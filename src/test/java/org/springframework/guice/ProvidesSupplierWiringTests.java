package org.springframework.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;
import org.springframework.guice.injector.SpringInjector;

import javax.inject.Singleton;
import java.util.function.Supplier;

/**
 * Test Generics (e.g., Supplier<T>) not losing type info across bridge in both directions
 *
 * @author Howard Yuan
 */
public class ProvidesSupplierWiringTests {

    //Test Guice -> Spring direction
    @Test
    public void testProvidesSupplier() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ModulesConfig.class, FooBar.class);
        Foo foo = (Foo)context.getBean(Foo.class);
        Bar bar = (Bar)context.getBean(Bar.class);
    }

    @Configuration
    @EnableGuiceModules
    static class ModulesConfig {
        @Bean
        TestConfig testConfig() {
            return new TestConfig();
        }
    }

    @Configuration
    static class FooBar {
        @Bean
        Foo foo(Supplier<Foo> supplier) {
            return supplier.get();
        }

        @Bean
        Bar bar(Supplier<Bar> supplier) {
            return supplier.get();
        }
    }

    static class TestConfig extends AbstractModule {
        @Override
        protected void configure() {
        }

        @Singleton
        @Provides
        Supplier<Foo> getFoo() {
            return ()->new Foo();
        }

        @Singleton
        @Provides
        Supplier<Bar> getBar() {
            return ()->new Bar();
        }
    }

    static class Foo {
    }

    static class Bar {

    }

    //Test Spring -> Guice direction
    //ToDo -- Today this direction doesn't work without further work. Ignore the test for now.
    @Ignore
    @Test
    public void testProvidesSupplierSpring() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(FooBarSpring.class);
        SpringInjector injector = new SpringInjector(context);
        Foo_Spring fooSpring = injector.getInstance(Key.get(new TypeLiteral<Supplier<Foo_Spring>>(){})).get();
        Bar_Spring barSpring = injector.getInstance(Key.get(new TypeLiteral<Supplier<Bar_Spring>>(){})).get();
    }

    @Configuration
    static class FooBarSpring {
        @Bean
        Supplier<Foo_Spring> fooSpring() {
            return ()->new Foo_Spring();
        }

        @Bean
        Bar_Spring barSpring() {
            return new Bar_Spring();
        }
    }

    static class Foo_Spring {
    }

    static class Bar_Spring {

    }

}
