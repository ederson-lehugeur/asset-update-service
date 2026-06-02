package com.invest.application;

import com.invest.domain.PriceVariationEngine;
import com.invest.domain.entities.Asset;
import com.invest.domain.events.UpdateAssetsEvent;
import com.invest.domain.ports.out.AssetPriceProvider;
import com.invest.domain.ports.out.AssetRepository;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class UpdateAssetsUseCaseImpl implements UpdateAssetsUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateAssetsUseCaseImpl.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final AssetPriceProvider assetPriceProvider;
    private final PriceVariationEngine priceVariationEngine;
    private final AssetRepository assetRepository;

    public UpdateAssetsUseCaseImpl(AssetPriceProvider assetPriceProvider,
                                   PriceVariationEngine priceVariationEngine,
                                   AssetRepository assetRepository) {
        this.assetPriceProvider = assetPriceProvider;
        this.priceVariationEngine = priceVariationEngine;
        this.assetRepository = assetRepository;
    }

    @Override
    public ProcessingResult execute(UpdateAssetsEvent event) {
        List<String> tickers = event.data().assets();
        log.info("Starting asset update processing. Total assets to process: {}", tickers.size());

        int successCount = 0;
        int failureCount = 0;

        for (String ticker : tickers) {
            try {
                processAsset(ticker);
                successCount++;
            } catch (Exception e) {
                log.error("Unexpected error processing ticker {}: {}", ticker, e.getMessage(), e);
                failureCount++;
            }
        }

        log.info("Asset update processing completed. Success: {}, Failures: {}", successCount, failureCount);
        return new ProcessingResult(successCount, failureCount);
    }

    private void processAsset(String ticker) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                Optional<Asset> optionalAsset = assetPriceProvider.fetchByTicker(ticker);

                if (optionalAsset.isEmpty()) {
                    log.warn("Ticker {} not found in database. Skipping.", ticker);
                    throw new AssetNotFoundException(ticker);
                }

                Asset currentAsset = optionalAsset.get();
                Asset updatedAsset = priceVariationEngine.applyVariation(currentAsset);

                log.info("Updating ticker {}: indicators {}", ticker, updatedAsset.getIndicators());

                assetRepository.save(updatedAsset);
                return;

            } catch (AssetNotFoundException e) {
                throw e;
            } catch (OptimisticLockException e) {
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    log.warn("Optimistic lock conflict for ticker {} on attempt {}/{}. Retrying...",
                            ticker, attempt, MAX_RETRY_ATTEMPTS);
                } else {
                    log.error("Optimistic lock conflict for ticker {} persisted after {} attempts. Giving up.",
                            ticker, MAX_RETRY_ATTEMPTS);
                    throw e;
                }
            }
        }
    }

    private static class AssetNotFoundException extends RuntimeException {
        AssetNotFoundException(String ticker) {
            super("Asset not found: " + ticker);
        }
    }
}
