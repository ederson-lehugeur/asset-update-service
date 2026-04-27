package com.invest.infrastructure.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;

@Slf4j
@Configuration
public class RetryConfig {

    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_INTERVAL_MS = 1000;
    private static final double MULTIPLIER = 2.0;
    private static final long MAX_INTERVAL_MS = 10000;

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter jackson2JsonMessageConverter,
            RetryOperationsInterceptor retryInterceptor) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jackson2JsonMessageConverter);
        factory.setAdviceChain(retryInterceptor);
        return factory;
    }

    @Bean
    RetryOperationsInterceptor retryInterceptor(AmqpTemplate amqpTemplate) {
        return org.springframework.amqp.rabbit.config.RetryInterceptorBuilder
                .stateless()
                .retryOperations(retryTemplate())
                .recoverer(republishMessageRecoverer(amqpTemplate))
                .build();
    }

    private RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(INITIAL_INTERVAL_MS);
        backOffPolicy.setMultiplier(MULTIPLIER);
        backOffPolicy.setMaxInterval(MAX_INTERVAL_MS);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(MAX_ATTEMPTS);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    private RepublishMessageRecoverer republishMessageRecoverer(AmqpTemplate amqpTemplate) {
        return new RepublishMessageRecoverer(amqpTemplate, RabbitMqConfig.DLX_EXCHANGE_NAME) {
            @Override
            public void recover(org.springframework.amqp.core.Message message, Throwable cause) {
                log.error("M=recover, E=Message routed to DLQ after retries exhausted, " +
                                "correlationId={}, tickers={}, error={}",
                        message.getMessageProperties().getHeader("correlationId"),
                        message.getMessageProperties().getHeader("tickers"),
                        cause.getMessage(), cause);
                super.recover(message, cause);
            }
        };
    }
}
