package org.springframework.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

import static org.junit.Assert.assertTrue;

public class SuperClassTests {

    @Test
    public void testSpringInterface() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ModulesConfig.class);
        IParent iParent = context.getBean(IParent.class);
        assertTrue(iParent instanceof IChildImpl);
    }

    @Test
    public void testGuiceInterface() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ModulesConfig.class);
        Injector injector = context.getBean(Injector.class);
        IParent iParent = injector.getInstance(IParent.class);
        assertTrue(iParent instanceof IChildImpl);
    }

    @Test
    public void testSpringClass() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ModulesConfig.class);
        IFoo iFoo = context.getBean(IFoo.class);
        assertTrue(iFoo instanceof Foo);
    }

    @Test
    public void testGuiceClass() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ModulesConfig.class);
        Injector injector = context.getBean(Injector.class);
        IFoo iFoo = injector.getInstance(IFoo.class);
        assertTrue(iFoo instanceof Foo);
    }

    @Configuration
    @EnableGuiceModules
    static class ModulesConfig {

        @Bean
        public IChild iChild() {
            return new IChildImpl();
        }

        @Bean
        public Foo iFoo() {
            return new Foo();
        }

        @Bean
        public AbstractModule disableJITModule() {
            return new AbstractModule() {
                @Override
                protected void configure() {
                    binder().requireExplicitBindings();
                }
            };
        }
    }


    public interface IParent {
    }

    public interface IChild extends IParent {

    }

    public static class IChildImpl implements IChild {

    }

    public interface IFoo {

    }

    public static class Foo implements IFoo {

    }
}
