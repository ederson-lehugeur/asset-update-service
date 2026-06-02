package com.invest.infrastructure.persistence.adapter;

import com.invest.domain.entities.Asset;
import com.invest.domain.entities.enumerator.IndicatorType;
import com.invest.domain.ports.out.AssetPriceProvider;
import com.invest.infrastructure.persistence.SpringDataAssetRepository;
import com.invest.infrastructure.persistence.entity.AssetIndicatorValueEntity;
import com.invest.infrastructure.persistence.entity.AssetJpaEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Component
public class AssetPriceProviderAdapter implements AssetPriceProvider {

    private final SpringDataAssetRepository springDataRepository;

    public AssetPriceProviderAdapter(SpringDataAssetRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Asset> fetchByTicker(String ticker) {
        return springDataRepository.findByTicker(ticker)
                .map(this::toDomainEntity);
    }

    private Asset toDomainEntity(AssetJpaEntity entity) {
        Map<IndicatorType, BigDecimal> indicators = new EnumMap<>(IndicatorType.class);
        for (AssetIndicatorValueEntity iv : entity.getIndicatorValues()) {
            IndicatorType.fromCode(iv.getId().getIndicatorType())
                    .ifPresent(type -> indicators.put(type, iv.getValue()));
        }
        return new Asset(entity.getId(), entity.getTicker(), indicators);
    }
}
