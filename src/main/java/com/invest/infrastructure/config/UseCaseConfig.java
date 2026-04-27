package com.invest.infrastructure.config;

import com.invest.application.UpdateAssetsUseCaseImpl;
import com.invest.domain.PriceVariationEngine;
import com.invest.domain.ports.out.AssetPriceProvider;
import com.invest.domain.ports.out.AssetRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class UseCaseConfig {

    @Bean
    PriceVariationEngine priceVariationEngine() {
        return new PriceVariationEngine(() -> new Random());
    }

    @Bean
    UpdateAssetsUseCaseImpl updateAssetsUseCase(AssetPriceProvider assetPriceProvider,
                                                 PriceVariationEngine priceVariationEngine,
                                                 AssetRepository assetRepository) {
        return new UpdateAssetsUseCaseImpl(assetPriceProvider, priceVariationEngine, assetRepository);
    }
}
