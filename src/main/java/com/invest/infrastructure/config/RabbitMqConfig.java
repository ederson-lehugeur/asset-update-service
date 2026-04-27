package com.invest.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE_NAME = "invest.assets.exchange";
    public static final String QUEUE_NAME = "invest.assets.update.queue";
    public static final String ROUTING_KEY = "asset.update";

    public static final String DLX_EXCHANGE_NAME = "invest.assets.dlx.exchange";
    public static final String DLQ_NAME = "invest.assets.update.dlq";

    @Bean
    DirectExchange assetsExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    Queue assetUpdateQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .build();
    }

    @Bean
    Binding assetUpdateBinding(Queue assetUpdateQueue, DirectExchange assetsExchange) {
        return BindingBuilder.bind(assetUpdateQueue).to(assetsExchange).with(ROUTING_KEY);
    }

    @Bean
    FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DLX_EXCHANGE_NAME);
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    Binding deadLetterBinding(Queue deadLetterQueue, FanoutExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange);
    }

    @Bean
    MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
