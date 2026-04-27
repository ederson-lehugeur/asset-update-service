package com.invest.domain;

import com.invest.domain.entities.Asset;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.util.function.Supplier;

public class PriceVariationEngine {

    private static final BigDecimal PRICE_FLOOR = new BigDecimal("0.01");
    private static final BigDecimal YIELD_FLOOR = BigDecimal.ZERO;
    private static final BigDecimal PVP_FLOOR = BigDecimal.ZERO;

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

        BigDecimal newPrice = vary(asset.getPrice(), random, PRICE_RANGE, PRICE_OFFSET);
        BigDecimal newDividendYield = vary(asset.getDividendYield(), random, SMALL_RANGE, SMALL_OFFSET);
        BigDecimal newPvp = vary(asset.getPvp(), random, SMALL_RANGE, SMALL_OFFSET);

        newPrice = clamp(newPrice, PRICE_FLOOR);
        newDividendYield = clamp(newDividendYield, YIELD_FLOOR);
        newPvp = clamp(newPvp, PVP_FLOOR);

        return new Asset(asset.getId(), asset.getTicker(), newPrice, newDividendYield, newPvp);
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
