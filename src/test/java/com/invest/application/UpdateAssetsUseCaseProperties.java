package com.invest.application;

import com.invest.domain.PriceVariationEngine;
import com.invest.domain.entities.Asset;
import com.invest.domain.entities.enumerator.IndicatorType;
import com.invest.domain.events.UpdateAssetsEvent;
import com.invest.domain.ports.out.AssetPriceProvider;
import com.invest.domain.ports.out.AssetRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdateAssetsUseCaseProperties {

    @Property(tries = 100)
    @Tag("Feature: asset-update-service, Property 4: Per-asset isolation")
    void perAssetIsolation(@ForAll("tickerScenarios") TickerScenario scenario) {
        AssetPriceProvider priceProvider = mock(AssetPriceProvider.class);
        PriceVariationEngine variationEngine = mock(PriceVariationEngine.class);
        AssetRepository repository = mock(AssetRepository.class);

        for (String ticker : scenario.tickers()) {
            if (scenario.existingTickers().contains(ticker)) {
                Map<IndicatorType, BigDecimal> indicators = new EnumMap<>(IndicatorType.class);
                indicators.put(IndicatorType.PRICE, BigDecimal.TEN);
                indicators.put(IndicatorType.DIVIDEND_YIELD, BigDecimal.ONE);
                indicators.put(IndicatorType.PVP, BigDecimal.ONE);
                Asset asset = new Asset(1L, ticker, indicators);
                when(priceProvider.fetchByTicker(ticker)).thenReturn(Optional.of(asset));
            } else {
                when(priceProvider.fetchByTicker(ticker)).thenReturn(Optional.empty());
            }
        }

        when(variationEngine.applyVariation(any(Asset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(Asset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateAssetsUseCase useCase = new UpdateAssetsUseCaseImpl(priceProvider, variationEngine, repository);
        UpdateAssetsEvent event = new UpdateAssetsEvent(
                "UPDATE_ASSETS",
                "test-correlation-id",
                new UpdateAssetsEvent.Data(scenario.tickers())
        );

        ProcessingResult result = useCase.execute(event);

        assertThat(result.successCount() + result.failureCount())
                .as("successCount + failureCount must equal total tickers")
                .isEqualTo(scenario.tickers().size());
        assertThat(result.successCount())
                .as("successCount must equal number of existing tickers")
                .isEqualTo(scenario.existingTickers().size());
        assertThat(result.failureCount())
                .as("failureCount must equal number of non-existing tickers")
                .isEqualTo(scenario.tickers().size() - scenario.existingTickers().size());
    }

    @Provide
    Arbitrary<TickerScenario> tickerScenarios() {
        Arbitrary<List<String>> tickerLists = Arbitraries.strings()
                .alpha().ofMinLength(4).ofMaxLength(8)
                .map(String::toUpperCase)
                .list().ofMinSize(1).ofMaxSize(10)
                .filter(list -> list.stream().distinct().count() == list.size());

        return tickerLists.flatMap(tickers -> {
            Arbitrary<Set<String>> existingSets = Arbitraries.of(tickers)
                    .set().ofMinSize(0).ofMaxSize(tickers.size());
            return existingSets.map(existing -> new TickerScenario(tickers, existing));
        });
    }

    record TickerScenario(List<String> tickers, Set<String> existingTickers) {}
}
