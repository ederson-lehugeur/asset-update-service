package com.invest.adapters.messaging;

import com.invest.application.ProcessingResult;
import com.invest.application.UpdateAssetsUseCase;
import com.invest.domain.events.UpdateAssetsEvent;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetUpdateListenerTest {

    @Mock
    private UpdateAssetsUseCase updateAssetsUseCase;

    @Mock
    private Channel channel;

    private AssetUpdateListener listener;

    private static final long DELIVERY_TAG = 42L;
    private static final String CORRELATION_ID = "test-corr-id";

    @BeforeEach
    void setUp() {
        listener = new AssetUpdateListener(updateAssetsUseCase);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    private UpdateAssetsEvent validEvent(List<String> tickers) {
        return new UpdateAssetsEvent("UPDATE_ASSETS", CORRELATION_ID, new UpdateAssetsEvent.Data(tickers));
    }

    private UpdateAssetsEvent eventWithType(String eventType, List<String> tickers) {
        return new UpdateAssetsEvent(eventType, CORRELATION_ID, new UpdateAssetsEvent.Data(tickers));
    }

    // Validates: Requirements 1.3, 1.4
    @Test
    @DisplayName("Valid message with UPDATE_ASSETS and non-empty assets delegates to use case and acks")
    void validMessageDelegatesToUseCaseAndAcks() throws IOException {
        UpdateAssetsEvent event = validEvent(List.of("MXRF11", "HGLG11"));
        when(updateAssetsUseCase.execute(event)).thenReturn(new ProcessingResult(2, 0));

        listener.onMessage(event, channel, DELIVERY_TAG);

        verify(updateAssetsUseCase).execute(event);
        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    // Validates: Requirements 2.2
    @Test
    @DisplayName("Invalid eventType nacks message and does not delegate to use case")
    void invalidEventTypeNacksMessage() throws IOException {
        UpdateAssetsEvent event = eventWithType("INVALID_TYPE", List.of("MXRF11"));

        listener.onMessage(event, channel, DELIVERY_TAG);

        verify(updateAssetsUseCase, never()).execute(any());
        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    // Validates: Requirements 2.1
    @Test
    @DisplayName("Null data nacks message and does not delegate to use case")
    void nullDataNacksMessage() throws IOException {
        UpdateAssetsEvent event = new UpdateAssetsEvent("UPDATE_ASSETS", CORRELATION_ID, null);

        listener.onMessage(event, channel, DELIVERY_TAG);

        verify(updateAssetsUseCase, never()).execute(any());
        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    // Validates: Requirements 2.1
    @Test
    @DisplayName("Null assets list nacks message and does not delegate to use case")
    void nullAssetsListNacksMessage() throws IOException {
        UpdateAssetsEvent event = new UpdateAssetsEvent("UPDATE_ASSETS", CORRELATION_ID,
                new UpdateAssetsEvent.Data(null));

        listener.onMessage(event, channel, DELIVERY_TAG);

        verify(updateAssetsUseCase, never()).execute(any());
        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    // Validates: Requirements 2.1
    @Test
    @DisplayName("Empty assets list nacks message and does not delegate to use case")
    void emptyAssetsListNacksMessage() throws IOException {
        UpdateAssetsEvent event = validEvent(Collections.emptyList());

        listener.onMessage(event, channel, DELIVERY_TAG);

        verify(updateAssetsUseCase, never()).execute(any());
        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    // Validates: Requirements 1.5
    @Test
    @DisplayName("Use case throws exception: nacks message")
    void useCaseExceptionNacksMessage() throws IOException {
        UpdateAssetsEvent event = validEvent(List.of("MXRF11"));
        when(updateAssetsUseCase.execute(event)).thenThrow(new RuntimeException("processing error"));

        listener.onMessage(event, channel, DELIVERY_TAG);

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    // Validates: Requirements 8.3
    @Test
    @DisplayName("MDC correlationId is set during processing and cleared after")
    void mdcCorrelationIdSetDuringProcessingAndClearedAfter() throws IOException {
        UpdateAssetsEvent event = validEvent(List.of("MXRF11"));
        when(updateAssetsUseCase.execute(any())).thenAnswer(inv -> {
            assertThat(MDC.get("correlationId")).isEqualTo(CORRELATION_ID);
            return new ProcessingResult(1, 0);
        });

        listener.onMessage(event, channel, DELIVERY_TAG);

        assertThat(MDC.get("correlationId")).isNull();
    }

    // Validates: Requirements 8.3
    @Test
    @DisplayName("MDC is cleared even when use case throws exception")
    void mdcClearedOnException() throws IOException {
        UpdateAssetsEvent event = validEvent(List.of("MXRF11"));
        when(updateAssetsUseCase.execute(any())).thenThrow(new RuntimeException("fail"));

        listener.onMessage(event, channel, DELIVERY_TAG);

        assertThat(MDC.get("correlationId")).isNull();
    }

    // Validates: Requirements 1.4
    @Test
    @DisplayName("basicAck is called with correct deliveryTag and multiple=false on success")
    void basicAckCalledWithCorrectDeliveryTag() throws IOException {
        long specificTag = 99L;
        UpdateAssetsEvent event = validEvent(List.of("MXRF11"));
        when(updateAssetsUseCase.execute(event)).thenReturn(new ProcessingResult(1, 0));

        listener.onMessage(event, channel, specificTag);

        verify(channel).basicAck(eq(99L), eq(false));
    }

    // Validates: Requirements 1.5
    @Test
    @DisplayName("basicNack is called with (deliveryTag, false, false) on failure")
    void basicNackCalledWithCorrectArguments() throws IOException {
        long specificTag = 77L;
        UpdateAssetsEvent event = eventWithType("WRONG", List.of("MXRF11"));

        listener.onMessage(event, channel, specificTag);

        verify(channel).basicNack(eq(77L), eq(false), eq(false));
    }

    // Validates: Requirements 1.5
    @Test
    @DisplayName("Nack IOException is swallowed gracefully")
    void nackIOExceptionIsSwallowed() throws IOException {
        UpdateAssetsEvent event = eventWithType("WRONG", List.of("MXRF11"));
        doThrow(new IOException("channel closed")).when(channel).basicNack(anyLong(), anyBoolean(), anyBoolean());

        listener.onMessage(event, channel, DELIVERY_TAG);

        verify(channel).basicNack(DELIVERY_TAG, false, false);
    }
}
