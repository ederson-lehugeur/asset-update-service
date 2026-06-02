package com.invest.application;

import com.invest.domain.PriceVariationEngine;
import com.invest.domain.entities.Asset;
import com.invest.domain.entities.enumerator.IndicatorType;
import com.invest.domain.events.UpdateAssetsEvent;
import com.invest.domain.ports.out.AssetPriceProvider;
import com.invest.domain.ports.out.AssetRepository;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateAssetsUseCaseImplTest {

    @Mock
    private AssetPriceProvider assetPriceProvider;

    @Mock
    private PriceVariationEngine priceVariationEngine;

    @Mock
    private AssetRepository assetRepository;

    private UpdateAssetsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateAssetsUseCaseImpl(assetPriceProvider, priceVariationEngine, assetRepository);
    }

    private UpdateAssetsEvent eventWith(List<String> tickers) {
        return new UpdateAssetsEvent("UPDATE_ASSETS", "corr-123", new UpdateAssetsEvent.Data(tickers));
    }

    private Asset asset(String ticker) {
        Map<IndicatorType, BigDecimal> indicators = new EnumMap<>(IndicatorType.class);
        indicators.put(IndicatorType.PRICE, BigDecimal.TEN);
        indicators.put(IndicatorType.DIVIDEND_YIELD, BigDecimal.ONE);
        indicators.put(IndicatorType.PVP, BigDecimal.ONE);
        return new Asset(1L, ticker, indicators);
    }

    private Asset updatedAsset(String ticker) {
        Map<IndicatorType, BigDecimal> indicators = new EnumMap<>(IndicatorType.class);
        indicators.put(IndicatorType.PRICE, new BigDecimal("10.03"));
        indicators.put(IndicatorType.DIVIDEND_YIELD, new BigDecimal("1.01"));
        indicators.put(IndicatorType.PVP, new BigDecimal("1.01"));
        return new Asset(1L, ticker, indicators);
    }

    @Test
    @DisplayName("Happy path: all tickers exist and update successfully")
    void allTickersExistAndUpdateSuccessfully() {
        List<String> tickers = List.of("MXRF11", "HGLG11", "XPLG11");

        for (String ticker : tickers) {
            when(assetPriceProvider.fetchByTicker(ticker)).thenReturn(Optional.of(asset(ticker)));
        }
        when(priceVariationEngine.applyVariation(any(Asset.class)))
                .thenAnswer(inv -> updatedAsset(((Asset) inv.getArgument(0)).getTicker()));
        when(assetRepository.save(any(Asset.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProcessingResult result = useCase.execute(eventWith(tickers));

        assertThat(result.successCount()).isEqualTo(3);
        assertThat(result.failureCount()).isZero();
        verify(assetRepository, times(3)).save(any(Asset.class));
    }

    @Test
    @DisplayName("Ticker not found: missing tickers count as failures, others succeed")
    void tickerNotFoundCountsAsFailure() {
        when(assetPriceProvider.fetchByTicker("MXRF11")).thenReturn(Optional.of(asset("MXRF11")));
        when(assetPriceProvider.fetchByTicker("UNKNOWN1")).thenReturn(Optional.empty());
        when(assetPriceProvider.fetchByTicker("HGLG11")).thenReturn(Optional.of(asset("HGLG11")));
        when(assetPriceProvider.fetchByTicker("UNKNOWN2")).thenReturn(Optional.empty());

        when(priceVariationEngine.applyVariation(any(Asset.class)))
                .thenAnswer(inv -> updatedAsset(((Asset) inv.getArgument(0)).getTicker()));
        when(assetRepository.save(any(Asset.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProcessingResult result = useCase.execute(eventWith(List.of("MXRF11", "UNKNOWN1", "HGLG11", "UNKNOWN2")));

        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isEqualTo(2);
        verify(assetRepository, times(2)).save(any(Asset.class));
    }

    @Test
    @DisplayName("Optimistic lock retry success: first attempt throws, second succeeds")
    void optimisticLockRetrySucceeds() {
        Asset original = asset("MXRF11");
        Asset updated = updatedAsset("MXRF11");

        when(assetPriceProvider.fetchByTicker("MXRF11")).thenReturn(Optional.of(original));
        when(priceVariationEngine.applyVariation(any(Asset.class))).thenReturn(updated);
        when(assetRepository.save(any(Asset.class)))
                .thenThrow(new OptimisticLockException("conflict"))
                .thenAnswer(inv -> inv.getArgument(0));

        ProcessingResult result = useCase.execute(eventWith(List.of("MXRF11")));

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isZero();
        verify(assetPriceProvider, times(2)).fetchByTicker("MXRF11");
        verify(assetRepository, times(2)).save(any(Asset.class));
    }

    @Test
    @DisplayName("Optimistic lock retry exhaustion: all 3 attempts fail counts as failure")
    void optimisticLockRetryExhaustion() {
        Asset original = asset("MXRF11");
        Asset updated = updatedAsset("MXRF11");

        when(assetPriceProvider.fetchByTicker("MXRF11")).thenReturn(Optional.of(original));
        when(priceVariationEngine.applyVariation(any(Asset.class))).thenReturn(updated);
        when(assetRepository.save(any(Asset.class)))
                .thenThrow(new OptimisticLockException("conflict"));

        ProcessingResult result = useCase.execute(eventWith(List.of("MXRF11")));

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        verify(assetPriceProvider, times(3)).fetchByTicker("MXRF11");
        verify(assetRepository, times(3)).save(any(Asset.class));
    }

    @Test
    @DisplayName("Mixed scenario: some succeed, some not found, some fail with exception")
    void mixedScenario() {
        when(assetPriceProvider.fetchByTicker("OK1")).thenReturn(Optional.of(asset("OK1")));
        when(assetPriceProvider.fetchByTicker("MISSING")).thenReturn(Optional.empty());
        when(assetPriceProvider.fetchByTicker("FAIL")).thenReturn(Optional.of(asset("FAIL")));

        when(priceVariationEngine.applyVariation(any(Asset.class)))
                .thenAnswer(inv -> updatedAsset(((Asset) inv.getArgument(0)).getTicker()));

        when(assetRepository.save(any(Asset.class)))
                .thenAnswer(inv -> {
                    Asset a = inv.getArgument(0);
                    if ("FAIL".equals(a.getTicker())) {
                        throw new OptimisticLockException("conflict");
                    }
                    return a;
                });

        ProcessingResult result = useCase.execute(eventWith(List.of("OK1", "MISSING", "FAIL")));

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(2);
        assertThat(result.successCount() + result.failureCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Empty ticker list: returns ProcessingResult(0, 0)")
    void emptyTickerListReturnsZeroCounts() {
        ProcessingResult result = useCase.execute(eventWith(List.of()));

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isZero();
        verify(assetPriceProvider, never()).fetchByTicker(any());
        verify(assetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Variation engine and repository are called for each existing ticker")
    void variationEngineAndRepositoryCalledPerTicker() {
        List<String> tickers = List.of("A", "B");

        for (String ticker : tickers) {
            when(assetPriceProvider.fetchByTicker(ticker)).thenReturn(Optional.of(asset(ticker)));
        }
        when(priceVariationEngine.applyVariation(any(Asset.class)))
                .thenAnswer(inv -> updatedAsset(((Asset) inv.getArgument(0)).getTicker()));
        when(assetRepository.save(any(Asset.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(eventWith(tickers));

        verify(priceVariationEngine, times(2)).applyVariation(any(Asset.class));
        verify(assetRepository, times(2)).save(any(Asset.class));
    }
}
