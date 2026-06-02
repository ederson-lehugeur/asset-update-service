package com.invest.domain;

import com.invest.domain.entities.Asset;
import com.invest.domain.entities.enumerator.IndicatorType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

public class PriceVariationEngine {

    private static final BigDecimal PRICE_FLOOR = new BigDecimal("0.01");
    private static final BigDecimal GENERAL_FLOOR = BigDecimal.ZERO;

    private static final double PRICE_RANGE = 0.10;
    private static final double PRICE_OFFSET = 0.05;
    private static final double SMALL_RANGE = 0.02;
    private static final double SMALL_OFFSET = 0.01;

    private static final int SCALE = 2;

    private final Supplier<Random> randomSupplier;

    public PriceVariationEngine(Supplier<Random> randomSupplier) {
        this.randomSupplier = randomSupplier;
    }

    public Asset applyVariation(Asset asset) {
        Random random = randomSupplier.get();
        Map<IndicatorType, BigDecimal> updatedIndicators = new EnumMap<>(IndicatorType.class);

        for (Map.Entry<IndicatorType, BigDecimal> entry : asset.getIndicators().entrySet()) {
            IndicatorType type = entry.getKey();
            BigDecimal current = entry.getValue();

            BigDecimal newValue;
            if (type == IndicatorType.PRICE) {
                newValue = vary(current, random, PRICE_RANGE, PRICE_OFFSET);
                newValue = clamp(newValue, PRICE_FLOOR);
            } else {
                newValue = vary(current, random, SMALL_RANGE, SMALL_OFFSET);
                newValue = clamp(newValue, GENERAL_FLOOR);
            }
            updatedIndicators.put(type, newValue);
        }

        return new Asset(asset.getId(), asset.getTicker(), updatedIndicators);
    }

    private BigDecimal vary(BigDecimal current, Random random, double range, double offset) {
        double variation = (random.nextDouble() * range) - offset;
        BigDecimal delta = BigDecimal.valueOf(variation);
        return current.add(delta).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal floor) {
        return value.compareTo(floor) < 0 ? floor : value;
    }
}
