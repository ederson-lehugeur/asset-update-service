package com.invest.infrastructure.persistence.adapter;

import com.invest.domain.entities.Asset;
import com.invest.domain.ports.out.AssetRepository;
import com.invest.infrastructure.persistence.SpringDataAssetRepository;
import com.invest.infrastructure.persistence.entity.AssetJpaEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AssetRepositoryAdapter implements AssetRepository {

    private final SpringDataAssetRepository springDataRepository;

    public AssetRepositoryAdapter(SpringDataAssetRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Asset save(Asset asset) {
        AssetJpaEntity entity = toJpaEntity(asset);
        entity.setUpdatedAt(LocalDateTime.now());
        AssetJpaEntity saved = springDataRepository.save(entity);
        return toDomainEntity(saved);
    }

    private AssetJpaEntity toJpaEntity(Asset asset) {
        AssetJpaEntity entity = new AssetJpaEntity();
        entity.setId(asset.getId());
        entity.setTicker(asset.getTicker());
        entity.setPrice(asset.getPrice());
        entity.setDividendYield(asset.getDividendYield());
        entity.setPvp(asset.getPvp());
        return entity;
    }

    private Asset toDomainEntity(AssetJpaEntity entity) {
        return new Asset(
                entity.getId(),
                entity.getTicker(),
                entity.getPrice(),
                entity.getDividendYield(),
                entity.getPvp()
        );
    }
}
