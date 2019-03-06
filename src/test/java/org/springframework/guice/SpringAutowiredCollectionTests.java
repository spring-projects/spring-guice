package org.springframework.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;
import org.springframework.guice.injector.SpringInjector;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SpringAutowiredCollectionTests {

    @Test
    public void getAutowiredCollection() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(TestConfig.class);
        context.refresh();
        Injector injector = new SpringInjector(context);

        ServicesHolder servicesHolder = injector.getInstance(ServicesHolder.class);

        assertEquals(2, servicesHolder.existingServices.size());
        assertEquals(0, servicesHolder.nonExistingServices.size());
    }

    @Configuration
    @EnableGuiceModules
    static class TestConfig {
        @Bean
        public ServicesHolder serviceHolder(Map<String, Service> existingServices,
                                            Map<String, NonExistingService> nonExistingServices) {
            return new ServicesHolder(existingServices, nonExistingServices);
        }

        @Bean
        public Service service() {
            return new Service();
        }

        @Bean
        public GuiceModule guiceServiceModule() {
            return new GuiceModule();
        }
    }

    static class Service {
    }

    static class NonExistingService {
    }

    static class GuiceModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(Service.class).asEagerSingleton();
        }
    }

    static class ServicesHolder {
        final Map<String, Service> existingServices;
        final Map<String, NonExistingService> nonExistingServices;

        public ServicesHolder(Map<String, Service> existingServices,
                              Map<String, NonExistingService> nonExistingServices) {
            this.existingServices = existingServices;
            this.nonExistingServices = nonExistingServices;
        }
    }
}
