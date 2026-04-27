package com.invest.domain.events;

import java.util.List;

public record UpdateAssetsEvent(
        String eventType,
        String correlationId,
        Data data
) {
    public record Data(List<String> assets) {}
}
