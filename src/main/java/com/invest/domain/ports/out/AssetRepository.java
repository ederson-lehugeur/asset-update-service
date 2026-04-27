package com.invest.domain.ports.out;

import com.invest.domain.entities.Asset;

public interface AssetRepository {

    Asset save(Asset asset);
}
