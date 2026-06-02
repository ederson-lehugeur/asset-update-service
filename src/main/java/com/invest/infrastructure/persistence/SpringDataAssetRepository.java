package com.invest.infrastructure.persistence;

import com.invest.infrastructure.persistence.entity.AssetJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpringDataAssetRepository extends JpaRepository<AssetJpaEntity, Long> {

    @Query("SELECT a FROM AssetJpaEntity a LEFT JOIN FETCH a.indicatorValues WHERE a.ticker = :ticker")
    Optional<AssetJpaEntity> findByTicker(@Param("ticker") String ticker);

    @Query("SELECT a FROM AssetJpaEntity a LEFT JOIN FETCH a.indicatorValues WHERE a.id = :id")
    Optional<AssetJpaEntity> findByIdWithIndicatorValues(@Param("id") Long id);
}
