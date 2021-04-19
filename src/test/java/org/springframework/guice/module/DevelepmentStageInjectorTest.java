package org.springframework.guice.module;

import com.google.inject.*;
import com.google.inject.Module;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;
import org.springframework.guice.annotation.InjectorFactory;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DevelepmentStageInjectorTest {

    @BeforeClass
    public static void init() {
        System.setProperty("spring.guice.stage", "DEVELOPMENT");
    }

    @AfterClass
    public static void cleanup() {
        System.clearProperty("spring.guice.stage");
    }

    @Test
    public void testLazyInitBean() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DevelepmentStageInjectorTest.ModulesConfig.class);
        TestGuiceModule testGuiceModule = context.getBean(TestGuiceModule.class);
        assertFalse(testGuiceModule.getProviderExecuted());
        GuiceToken guiceToken = context.getBean(GuiceToken.class);
        assertTrue(testGuiceModule.getProviderExecuted());
        context.close();
    }

    @Configuration
    @EnableGuiceModules
    static class ModulesConfig {
        @Bean
        public TestGuiceModule testGuiceModule() {
            return new TestGuiceModule();
        }

        @Bean
        public InjectorFactory injectorFactory() {
            return new TestDevelopmentStageInjectorFactory();
        }

    }

    static class TestGuiceModule extends AbstractModule {
        private boolean providerExecuted = false;

        public boolean getProviderExecuted() {
            return this.providerExecuted;
        }

        @Override
        protected void configure() {
        }

        @Provides
        @Singleton
        public GuiceToken guiceToken() {
            this.providerExecuted = true;
            return new GuiceToken();
        }
    }

    static class TestDevelopmentStageInjectorFactory implements InjectorFactory {

        @Override
        public Injector createInjector(List<Module> modules) {
            return Guice.createInjector(Stage.DEVELOPMENT, modules);
        }
    }

    static class GuiceToken {}
}
