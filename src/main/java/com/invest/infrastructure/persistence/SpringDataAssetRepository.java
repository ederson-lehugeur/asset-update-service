package com.invest.infrastructure.persistence;

import com.invest.infrastructure.persistence.entity.AssetJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataAssetRepository extends JpaRepository<AssetJpaEntity, Long> {

    Optional<AssetJpaEntity> findByTicker(String ticker);
}
