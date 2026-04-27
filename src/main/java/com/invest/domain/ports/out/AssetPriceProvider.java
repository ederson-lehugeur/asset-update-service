package com.invest.domain.ports.out;

import com.invest.domain.entities.Asset;

import java.util.Optional;

public interface AssetPriceProvider {

    Optional<Asset> fetchByTicker(String ticker);
}
