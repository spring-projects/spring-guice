package org.springframework.guice;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

import javax.inject.Inject;

import static org.junit.Assert.assertNotNull;

public class JustInTimeBindingTests {

    @After
    public void tearDown() {
        System.clearProperty("spring.guice.autowireJIT");
    }

    @Test
    public void springWithJustInTimeBinding() {
        System.setProperty("spring.guice.autowireJIT", "true");
        assertNotNull(springGetFoo());
    }

    @Test(expected = UnsatisfiedDependencyException.class)
    public void springWithoutJustInTimeBinding() {
        System.setProperty("spring.guice.autowireJIT", "false");
        springGetFoo();
    }

    private Foo springGetFoo() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ModulesConfig.class);
        context.getDefaultListableBeanFactory().registerBeanDefinition(Foo.class.getSimpleName(), new RootBeanDefinition(Foo.class));
        return context.getBean(Foo.class);
    }

    @Configuration
    @EnableGuiceModules
    static class ModulesConfig {

    }


    public static class Service {
    }

    public static class Foo {

        Service service;

        @Inject
        public Foo(Service service) {
            this.service = service;
        }

    }

}
