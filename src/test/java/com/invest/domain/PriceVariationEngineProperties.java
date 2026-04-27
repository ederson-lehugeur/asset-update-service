package com.invest.domain;

import com.invest.domain.entities.Asset;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

import java.math.BigDecimal;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class PriceVariationEngineProperties {

    private final PriceVariationEngine engine = new PriceVariationEngine(Random::new);

    /**
     * Validates: Requirements 3.1, 3.2, 3.3
     *
     * For any asset with valid price, dividendYield, and pvp, after applyVariation(),
     * the difference between new and original price is within [-0.05, +0.05],
     * dividendYield within [-0.01, +0.01], pvp within [-0.01, +0.01].
     * Uses values large enough that floor clamping won't trigger.
     */
    @Property(tries = 100)
    @Tag("Feature: asset-update-service, Property 1: Variation bounds")
    void variationBounds(@ForAll("highValueAssets") Asset asset) {
        Asset result = engine.applyVariation(asset);

        BigDecimal priceDiff = result.getPrice().subtract(asset.getPrice());
        BigDecimal yieldDiff = result.getDividendYield().subtract(asset.getDividendYield());
        BigDecimal pvpDiff = result.getPvp().subtract(asset.getPvp());

        assertThat(priceDiff.doubleValue())
                .isBetween(-0.05, 0.05);
        assertThat(yieldDiff.doubleValue())
                .isBetween(-0.01, 0.01);
        assertThat(pvpDiff.doubleValue())
                .isBetween(-0.01, 0.01);
    }

    /**
     * Validates: Requirements 3.4
     *
     * For any asset, after applyVariation(), all three output values
     * have a scale of at most 2 decimal places.
     */
    @Property(tries = 100)
    @Tag("Feature: asset-update-service, Property 2: Rounding invariant")
    void roundingInvariant(@ForAll("anyAssets") Asset asset) {
        Asset result = engine.applyVariation(asset);

        assertThat(result.getPrice().scale()).isLessThanOrEqualTo(2);
        assertThat(result.getDividendYield().scale()).isLessThanOrEqualTo(2);
        assertThat(result.getPvp().scale()).isLessThanOrEqualTo(2);
    }

    /**
     * Validates: Requirements 3.5, 3.6, 3.7
     *
     * For any asset (including values very close to zero), after applyVariation(),
     * price >= 0.01, dividendYield >= 0.00, pvp >= 0.00.
     */
    @Property(tries = 100)
    @Tag("Feature: asset-update-service, Property 3: Floor constraints")
    void floorConstraints(@ForAll("lowValueAssets") Asset asset) {
        Asset result = engine.applyVariation(asset);

        assertThat(result.getPrice()).isGreaterThanOrEqualTo(new BigDecimal("0.01"));
        assertThat(result.getDividendYield()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(result.getPvp()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Provide
    Arbitrary<Asset> highValueAssets() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 10000),
                Arbitraries.strings().alpha().ofMinLength(4).ofMaxLength(8),
                decimals(1.00, 1000.00),
                decimals(1.00, 10.00),
                decimals(1.00, 10.00)
        ).as(Asset::new);
    }

    @Provide
    Arbitrary<Asset> anyAssets() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 10000),
                Arbitraries.strings().alpha().ofMinLength(4).ofMaxLength(8),
                decimals(0.01, 1000.00),
                decimals(0.00, 10.00),
                decimals(0.00, 10.00)
        ).as(Asset::new);
    }

    @Provide
    Arbitrary<Asset> lowValueAssets() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 10000),
                Arbitraries.strings().alpha().ofMinLength(4).ofMaxLength(8),
                decimals(0.01, 0.10),
                decimals(0.00, 0.05),
                decimals(0.00, 0.05)
        ).as(Asset::new);
    }

    private Arbitrary<BigDecimal> decimals(double min, double max) {
        return Arbitraries.doubles()
                .between(min, max)
                .ofScale(2)
                .map(BigDecimal::valueOf);
    }
}
