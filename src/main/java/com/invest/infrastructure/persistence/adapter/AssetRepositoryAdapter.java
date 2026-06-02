package com.invest.infrastructure.persistence.adapter;

import com.invest.domain.entities.Asset;
import com.invest.domain.entities.enumerator.IndicatorType;
import com.invest.domain.ports.out.AssetRepository;
import com.invest.infrastructure.persistence.SpringDataAssetRepository;
import com.invest.infrastructure.persistence.entity.AssetIndicatorValueEntity;
import com.invest.infrastructure.persistence.entity.AssetIndicatorValueId;
import com.invest.infrastructure.persistence.entity.AssetJpaEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class AssetRepositoryAdapter implements AssetRepository {

    private final SpringDataAssetRepository springDataRepository;

    public AssetRepositoryAdapter(SpringDataAssetRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Asset save(Asset asset) {
        AssetJpaEntity entity = springDataRepository.findByIdWithIndicatorValues(asset.getId())
                .orElseThrow(() -> new IllegalArgumentException("Asset not found with id: " + asset.getId()));

        entity.setUpdatedAt(LocalDateTime.now());

        for (Map.Entry<IndicatorType, BigDecimal> entry : asset.getIndicators().entrySet()) {
            String code = entry.getKey().code();
            BigDecimal newValue = entry.getValue();

            entity.getIndicatorValues().stream()
                    .filter(iv -> iv.getId().getIndicatorType().equals(code))
                    .findFirst()
                    .ifPresentOrElse(
                            iv -> iv.setValue(newValue),
                            () -> {
                                AssetIndicatorValueEntity newIv = new AssetIndicatorValueEntity();
                                newIv.setId(new AssetIndicatorValueId(entity.getId(), code));
                                newIv.setAsset(entity);
                                newIv.setValue(newValue);
                                entity.getIndicatorValues().add(newIv);
                            }
                    );
        }

        AssetJpaEntity saved = springDataRepository.save(entity);
        return toDomainEntity(saved);
    }

    private Asset toDomainEntity(AssetJpaEntity entity) {
        Map<IndicatorType, BigDecimal> indicators = new java.util.EnumMap<>(IndicatorType.class);
        for (AssetIndicatorValueEntity iv : entity.getIndicatorValues()) {
            IndicatorType.fromCode(iv.getId().getIndicatorType())
                    .ifPresent(type -> indicators.put(type, iv.getValue()));
        }
        return new Asset(entity.getId(), entity.getTicker(), indicators);
    }
}
