package com.invest.domain.entities;

import com.invest.domain.entities.enumerator.IndicatorType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AssetTest {

    @Test
    void shouldCreateAssetWithAllIndicators() {
        Map<IndicatorType, BigDecimal> indicators = new EnumMap<>(IndicatorType.class);
        indicators.put(IndicatorType.PRICE, new BigDecimal("10.50"));
        indicators.put(IndicatorType.DIVIDEND_YIELD, new BigDecimal("0.85"));
        indicators.put(IndicatorType.PVP, new BigDecimal("1.02"));

        var asset = new Asset(1L, "MXRF11", indicators);

        assertThat(asset.getId()).isEqualTo(1L);
        assertThat(asset.getTicker()).isEqualTo("MXRF11");
        assertThat(asset.getIndicator(IndicatorType.PRICE)).isEqualByComparingTo("10.50");
        assertThat(asset.getIndicator(IndicatorType.DIVIDEND_YIELD)).isEqualByComparingTo("0.85");
        assertThat(asset.getIndicator(IndicatorType.PVP)).isEqualByComparingTo("1.02");
    }

    @Test
    void shouldUpdateIndicator() {
        Map<IndicatorType, BigDecimal> indicators = new EnumMap<>(IndicatorType.class);
        indicators.put(IndicatorType.PRICE, new BigDecimal("10.50"));

        var asset = new Asset(1L, "MXRF11", indicators);
        asset.updateIndicator(IndicatorType.PRICE, new BigDecimal("11.00"));

        assertThat(asset.getIndicator(IndicatorType.PRICE)).isEqualByComparingTo("11.00");
    }

    @Test
    void shouldPreserveImmutableFieldsAfterUpdate() {
        Map<IndicatorType, BigDecimal> indicators = new EnumMap<>(IndicatorType.class);
        indicators.put(IndicatorType.PRICE, new BigDecimal("10.50"));

        var asset = new Asset(1L, "MXRF11", indicators);
        asset.updateIndicator(IndicatorType.PRICE, new BigDecimal("11.00"));

        assertThat(asset.getId()).isEqualTo(1L);
        assertThat(asset.getTicker()).isEqualTo("MXRF11");
    }

    @Test
    void shouldReturnDefensiveCopyOfIndicators() {
        Map<IndicatorType, BigDecimal> indicators = new EnumMap<>(IndicatorType.class);
        indicators.put(IndicatorType.PRICE, new BigDecimal("10.50"));

        var asset = new Asset(1L, "MXRF11", indicators);
        Map<IndicatorType, BigDecimal> copy = asset.getIndicators();

        assertThat(copy).containsEntry(IndicatorType.PRICE, new BigDecimal("10.50"));
    }
}
