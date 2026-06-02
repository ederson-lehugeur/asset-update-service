package com.invest.domain;

import com.invest.domain.entities.Asset;
import com.invest.domain.entities.enumerator.IndicatorType;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class PriceVariationEngineProperties {

    private final PriceVariationEngine engine = new PriceVariationEngine(Random::new);

    @Property(tries = 100)
    @Tag("Feature: asset-update-service, Property 1: Variation bounds")
    void variationBounds(@ForAll("highValueAssets") Asset asset) {
        Asset result = engine.applyVariation(asset);

        BigDecimal priceDiff = result.getIndicator(IndicatorType.PRICE)
                .subtract(asset.getIndicator(IndicatorType.PRICE));
        BigDecimal yieldDiff = result.getIndicator(IndicatorType.DIVIDEND_YIELD)
                .subtract(asset.getIndicator(IndicatorType.DIVIDEND_YIELD));
        BigDecimal pvpDiff = result.getIndicator(IndicatorType.PVP)
                .subtract(asset.getIndicator(IndicatorType.PVP));

        assertThat(priceDiff.doubleValue()).isBetween(-0.05, 0.05);
        assertThat(yieldDiff.doubleValue()).isBetween(-0.01, 0.01);
        assertThat(pvpDiff.doubleValue()).isBetween(-0.01, 0.01);
    }

    @Property(tries = 100)
    @Tag("Feature: asset-update-service, Property 2: Rounding invariant")
    void roundingInvariant(@ForAll("anyAssets") Asset asset) {
        Asset result = engine.applyVariation(asset);

        for (BigDecimal value : result.getIndicators().values()) {
            assertThat(value.scale()).isLessThanOrEqualTo(2);
        }
    }

    @Property(tries = 100)
    @Tag("Feature: asset-update-service, Property 3: Floor constraints")
    void floorConstraints(@ForAll("lowValueAssets") Asset asset) {
        Asset result = engine.applyVariation(asset);

        assertThat(result.getIndicator(IndicatorType.PRICE))
                .isGreaterThanOrEqualTo(new BigDecimal("0.01"));
        assertThat(result.getIndicator(IndicatorType.DIVIDEND_YIELD))
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(result.getIndicator(IndicatorType.PVP))
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Provide
    Arbitrary<Asset> highValueAssets() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 10000),
                Arbitraries.strings().alpha().ofMinLength(4).ofMaxLength(8),
                decimals(1.00, 1000.00),
                decimals(1.00, 10.00),
                decimals(1.00, 10.00)
        ).as((id, ticker, price, dy, pvp) -> buildAsset(id, ticker, price, dy, pvp));
    }

    @Provide
    Arbitrary<Asset> anyAssets() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 10000),
                Arbitraries.strings().alpha().ofMinLength(4).ofMaxLength(8),
                decimals(0.01, 1000.00),
                decimals(0.00, 10.00),
                decimals(0.00, 10.00)
        ).as((id, ticker, price, dy, pvp) -> buildAsset(id, ticker, price, dy, pvp));
    }

    @Provide
    Arbitrary<Asset> lowValueAssets() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 10000),
                Arbitraries.strings().alpha().ofMinLength(4).ofMaxLength(8),
                decimals(0.01, 0.10),
                decimals(0.00, 0.05),
                decimals(0.00, 0.05)
        ).as((id, ticker, price, dy, pvp) -> buildAsset(id, ticker, price, dy, pvp));
    }

    private Asset buildAsset(Long id, String ticker, BigDecimal price, BigDecimal dy, BigDecimal pvp) {
        Map<IndicatorType, BigDecimal> indicators = new EnumMap<>(IndicatorType.class);
        indicators.put(IndicatorType.PRICE, price);
        indicators.put(IndicatorType.DIVIDEND_YIELD, dy);
        indicators.put(IndicatorType.PVP, pvp);
        return new Asset(id, ticker, indicators);
    }

    private Arbitrary<BigDecimal> decimals(double min, double max) {
        return Arbitraries.doubles()
                .between(min, max)
                .ofScale(2)
                .map(BigDecimal::valueOf);
    }
}
