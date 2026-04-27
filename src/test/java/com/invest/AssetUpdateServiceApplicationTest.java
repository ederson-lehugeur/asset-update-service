package com.invest;

import com.invest.infrastructure.config.RabbitMqConfig;
import com.invest.infrastructure.config.RetryConfig;
import com.invest.infrastructure.config.UseCaseConfig;

import org.junit.jupiter.api.Test;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class AssetUpdateServiceApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertNotNull(applicationContext);
    }

    @Test
    void rabbitMqConfigurationBeansAreRegistered() {
        assertNotNull(applicationContext.getBean(RabbitMqConfig.class));
        assertNotNull(applicationContext.getBean(Jackson2JsonMessageConverter.class));
    }

    @Test
    void retryConfigBeanIsRegistered() {
        assertNotNull(applicationContext.getBean(RetryConfig.class));
    }

    @Test
    void useCaseConfigBeanIsRegistered() {
        assertNotNull(applicationContext.getBean(UseCaseConfig.class));
    }
}
