package com.invest.domain.entities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AssetTest {

    @Test
    void shouldCreateAssetWithAllFields() {
        var asset = new Asset(1L, "MXRF11", new BigDecimal("10.50"), new BigDecimal("0.85"), new BigDecimal("1.02"));

        assertThat(asset.getId()).isEqualTo(1L);
        assertThat(asset.getTicker()).isEqualTo("MXRF11");
        assertThat(asset.getPrice()).isEqualByComparingTo("10.50");
        assertThat(asset.getDividendYield()).isEqualByComparingTo("0.85");
        assertThat(asset.getPvp()).isEqualByComparingTo("1.02");
    }

    @Test
    void shouldUpdateMutableFieldsViaUpdateValues() {
        var asset = new Asset(1L, "MXRF11", new BigDecimal("10.50"), new BigDecimal("0.85"), new BigDecimal("1.02"));

        asset.updateValues(new BigDecimal("11.00"), new BigDecimal("0.90"), new BigDecimal("1.10"));

        assertThat(asset.getPrice()).isEqualByComparingTo("11.00");
        assertThat(asset.getDividendYield()).isEqualByComparingTo("0.90");
        assertThat(asset.getPvp()).isEqualByComparingTo("1.10");
    }

    @Test
    void shouldPreserveImmutableFieldsAfterUpdate() {
        var asset = new Asset(1L, "MXRF11", new BigDecimal("10.50"), new BigDecimal("0.85"), new BigDecimal("1.02"));

        asset.updateValues(new BigDecimal("11.00"), new BigDecimal("0.90"), new BigDecimal("1.10"));

        assertThat(asset.getId()).isEqualTo(1L);
        assertThat(asset.getTicker()).isEqualTo("MXRF11");
    }

    @Test
    void shouldAllowMultipleUpdates() {
        var asset = new Asset(1L, "HGLG11", new BigDecimal("150.00"), new BigDecimal("0.70"), new BigDecimal("0.95"));

        asset.updateValues(new BigDecimal("155.00"), new BigDecimal("0.75"), new BigDecimal("1.00"));
        asset.updateValues(new BigDecimal("148.00"), new BigDecimal("0.68"), new BigDecimal("0.92"));

        assertThat(asset.getPrice()).isEqualByComparingTo("148.00");
        assertThat(asset.getDividendYield()).isEqualByComparingTo("0.68");
        assertThat(asset.getPvp()).isEqualByComparingTo("0.92");
    }
}
