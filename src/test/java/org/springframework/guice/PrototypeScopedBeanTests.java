package org.springframework.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.junit.Test;
import org.springframework.context.annotation.*;
import org.springframework.guice.annotation.EnableGuiceModules;

import javax.inject.Inject;

import static org.junit.Assert.*;

public class PrototypeScopedBeanTests {

    @Test
    public void testPrototypeScopedBeans() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                ModulesConfig.class);
        Injector injector = context.getBean(Injector.class);
        GuiceService1 gs1 = injector.getInstance(GuiceService1.class);
        GuiceService2 gs2 = injector.getInstance((GuiceService2.class));
        assertNotNull(gs1);
        assertNotNull(gs2);
        assertNotEquals(gs1.bean, gs2.bean);
    }

    @Configuration
    @EnableGuiceModules
    static class ModulesConfig {

        @Bean
        public Module guiceModule() {
            return new AbstractModule() {
                @Override
                protected void configure() {
                    bind(GuiceService1.class).asEagerSingleton();
                    bind(GuiceService2.class).asEagerSingleton();
                }
            };
        }

        @Bean
        @Scope("prototype")
        public PrototypeBean prototypeBean() {
            return new PrototypeBean();
        }
    }

    public static class PrototypeBean {}
    public static class GuiceService1 {
        @Inject PrototypeBean bean;
    }
    public static class GuiceService2 {
        @Inject PrototypeBean bean;
    }
}
