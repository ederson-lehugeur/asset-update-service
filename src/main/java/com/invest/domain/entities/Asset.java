package com.invest.domain.entities;

import com.invest.domain.entities.enumerator.IndicatorType;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

public class Asset {

    private final Long id;
    private final String ticker;
    private final Map<IndicatorType, BigDecimal> indicators;

    public Asset(Long id, String ticker, Map<IndicatorType, BigDecimal> indicators) {
        this.id = id;
        this.ticker = ticker;
        this.indicators = new EnumMap<>(indicators);
    }

    public void updateIndicator(IndicatorType type, BigDecimal value) {
        this.indicators.put(type, value);
    }

    public BigDecimal getIndicator(IndicatorType type) {
        return indicators.get(type);
    }

    public Map<IndicatorType, BigDecimal> getIndicators() {
        return Map.copyOf(indicators);
    }

    public Long getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }
}
