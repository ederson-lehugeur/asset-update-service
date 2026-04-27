package com.invest.adapters.messaging;

import com.invest.application.UpdateAssetsUseCase;
import com.invest.domain.events.UpdateAssetsEvent;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetUpdateListener {

    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String EXPECTED_EVENT_TYPE = "UPDATE_ASSETS";

    private final UpdateAssetsUseCase updateAssetsUseCase;

    @RabbitListener(queues = "${rabbitmq.queue.asset-update}", ackMode = "MANUAL")
    public void onMessage(UpdateAssetsEvent event,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            MDC.put(CORRELATION_ID_KEY, event.correlationId());

            if (!EXPECTED_EVENT_TYPE.equals(event.eventType())) {
                log.warn("M=onMessage, I=Invalid eventType, eventType={}, correlationId={}",
                        event.eventType(), event.correlationId());
                nack(channel, deliveryTag);
                return;
            }

            if (event.data() == null || event.data().assets() == null || event.data().assets().isEmpty()) {
                log.warn("M=onMessage, I=Assets list is null or empty, correlationId={}", event.correlationId());
                nack(channel, deliveryTag);
                return;
            }

            log.info("M=onMessage, I=Received asset update event, correlationId={}, assetCount={}",
                    event.correlationId(), event.data().assets().size());

            updateAssetsUseCase.execute(event);

            channel.basicAck(deliveryTag, false);
            log.info("M=onMessage, I=Message acknowledged, correlationId={}", event.correlationId());
        } catch (Exception exception) {
            log.error("M=onMessage, E=Failed to process asset update, correlationId={}, error={}",
                    event.correlationId(), exception.getMessage(), exception);
            nack(channel, deliveryTag);
        } finally {
            MDC.clear();
        }
    }

    private void nack(Channel channel, long deliveryTag) {
        try {
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException nackException) {
            log.error("M=nack, E=Failed to nack message", nackException);
        }
    }
}
