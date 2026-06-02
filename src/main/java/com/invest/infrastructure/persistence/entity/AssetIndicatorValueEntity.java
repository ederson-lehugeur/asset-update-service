package com.invest.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "asset_indicator_value")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssetIndicatorValueEntity {

    @EmbeddedId
    private AssetIndicatorValueId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("assetId")
    @JoinColumn(name = "asset_id")
    private AssetJpaEntity asset;

    @Column(name = "value", nullable = false, precision = 19, scale = 4)
    private BigDecimal value;
}
