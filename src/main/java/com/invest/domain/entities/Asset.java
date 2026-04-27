package com.invest.domain.entities;

import java.math.BigDecimal;

public class Asset {

    private final Long id;
    private final String ticker;
    private BigDecimal price;
    private BigDecimal dividendYield;
    private BigDecimal pvp;

    public Asset(Long id, String ticker, BigDecimal price, BigDecimal dividendYield, BigDecimal pvp) {
        this.id = id;
        this.ticker = ticker;
        this.price = price;
        this.dividendYield = dividendYield;
        this.pvp = pvp;
    }

    public void updateValues(BigDecimal newPrice, BigDecimal newDividendYield, BigDecimal newPvp) {
        this.price = newPrice;
        this.dividendYield = newDividendYield;
        this.pvp = newPvp;
    }

    public Long getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getDividendYield() {
        return dividendYield;
    }

    public BigDecimal getPvp() {
        return pvp;
    }
}
