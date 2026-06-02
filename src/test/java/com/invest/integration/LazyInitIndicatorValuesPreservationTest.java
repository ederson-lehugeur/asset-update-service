package com.invest.integration;

import com.invest.domain.entities.Asset;
import com.invest.domain.entities.enumerator.IndicatorType;
import com.invest.infrastructure.persistence.SpringDataAssetRepository;
import com.invest.infrastructure.persistence.adapter.AssetPriceProviderAdapter;
import com.invest.infrastructure.persistence.adapter.AssetRepositoryAdapter;
import com.invest.infrastructure.persistence.entity.AssetIndicatorValueEntity;
import com.invest.infrastructure.persistence.entity.AssetIndicatorValueId;
import com.invest.infrastructure.persistence.entity.AssetJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.BeforeContainer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.containers.MySQLContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Preservation property tests for LazyInitializationException bugfix.
 *
 * These tests verify non-buggy paths that must remain unchanged after the fix:
 * - findByTicker() with LEFT JOIN FETCH continues to work
 * - New indicator creation works (tested via findByTicker path)
 * - Existing indicator update works (tested via findByTicker path)
 * - save() with non-existent id throws IllegalArgumentException
 *
 * EXPECTED OUTCOME: All tests PASS on UNFIXED code (confirms baseline to preserve).
 *
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4
 */
class LazyInitIndicatorValuesPreservationTest {

    @Configuration
    @EnableAutoConfiguration(exclude = {RabbitAutoConfiguration.class})
    @EntityScan(basePackages = "com.invest.infrastructure.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.invest.infrastructure.persistence")
    @ComponentScan(
            basePackages = "com.invest.infrastructure.persistence.adapter",
            excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*PriceProvider.*")
    )
    static class TestConfig {
    }

    @SuppressWarnings("resource")
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    private static ConfigurableApplicationContext context;
    private static AssetRepositoryAdapter assetRepositoryAdapter;
    private static SpringDataAssetRepository springDataRepository;
    private static EntityManagerFactory entityManagerFactory;
    private static Long seededAssetId;

    @BeforeContainer
    static void startContainerAndContext() {
        mysql.start();

        context = new SpringApplicationBuilder(TestConfig.class)
                .run("--spring.datasource.url=" + mysql.getJdbcUrl(),
                     "--spring.datasource.username=" + mysql.getUsername(),
                     "--spring.datasource.password=" + mysql.getPassword(),
                     "--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                     "--spring.jpa.hibernate.ddl-auto=create",
                     "--spring.jpa.open-in-view=false",
                     "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
                     "--spring.sql.init.mode=never");

        assetRepositoryAdapter = context.getBean(AssetRepositoryAdapter.class);
        springDataRepository = context.getBean(SpringDataAssetRepository.class);
        entityManagerFactory = context.getBean(EntityManagerFactory.class);

        seedTestAsset();
    }

    @AfterContainer
    static void stopContainerAndContext() {
        if (context != null) {
            context.close();
        }
        mysql.stop();
    }

    private static void seedTestAsset() {
        EntityManager em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();

        AssetJpaEntity asset = new AssetJpaEntity();
        asset.setTicker("AAPL");
        asset.setName("Apple Inc");
        asset.setAssetType("STOCK");
        asset.setUpdatedAt(LocalDateTime.now());
        em.persist(asset);
        em.flush();

        seededAssetId = asset.getId();

        AssetIndicatorValueEntity priceIndicator = new AssetIndicatorValueEntity();
        priceIndicator.setId(new AssetIndicatorValueId(seededAssetId, IndicatorType.PRICE.code()));
        priceIndicator.setAsset(asset);
        priceIndicator.setValue(new BigDecimal("150.0000"));
        em.persist(priceIndicator);

        AssetIndicatorValueEntity dyIndicator = new AssetIndicatorValueEntity();
        dyIndicator.setId(new AssetIndicatorValueId(seededAssetId, IndicatorType.DIVIDEND_YIELD.code()));
        dyIndicator.setAsset(asset);
        dyIndicator.setValue(new BigDecimal("2.5000"));
        em.persist(dyIndicator);

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Property 2a: FindByTicker Preservation
     *
     * For any asset in the database, findByTicker() eagerly fetches indicatorValues
     * via the existing LEFT JOIN FETCH query. The returned entity has all indicator
     * values accessible without a LazyInitializationException.
     *
     * This path is NOT affected by the bug (findByTicker already uses JOIN FETCH).
     *
     * Validates: Requirements 3.1
     */
    @Property(tries = 10)
    void findByTickerEagerlyFetchesIndicatorValues(
            @ForAll("randomIndicatorValues") Map<IndicatorType, BigDecimal> expectedUpdates
    ) {
        updateIndicatorsViaEntityManager(expectedUpdates);

        Optional<AssetJpaEntity> result = springDataRepository.findByTicker("AAPL");

        assertThat(result).isPresent();
        AssetJpaEntity entity = result.get();
        assertThat(entity.getId()).isEqualTo(seededAssetId);
        assertThat(entity.getTicker()).isEqualTo("AAPL");
        assertThat(entity.getIndicatorValues()).isNotNull();
        assertThat(entity.getIndicatorValues().size()).isGreaterThanOrEqualTo(expectedUpdates.size());

        for (Map.Entry<IndicatorType, BigDecimal> entry : expectedUpdates.entrySet()) {
            Optional<AssetIndicatorValueEntity> indicator = entity.getIndicatorValues().stream()
                    .filter(iv -> iv.getId().getIndicatorType().equals(entry.getKey().code()))
                    .findFirst();
            assertThat(indicator)
                    .as("Indicator %s should be present after findByTicker", entry.getKey())
                    .isPresent();
            assertThat(indicator.get().getValue())
                    .as("Indicator %s should have value %s", entry.getKey(), entry.getValue())
                    .isEqualByComparingTo(entry.getValue());
        }
    }

    /**
     * Property 2b: New Indicator Creation Preservation
     *
     * When a new indicator type is added to an asset (via direct entity manipulation
     * simulating what save() does internally), the new AssetIndicatorValueEntity is
     * correctly persisted and visible via findByTicker().
     *
     * This tests the creation path using findByTicker which works on unfixed code.
     *
     * Validates: Requirements 3.2
     */
    @Property(tries = 10)
    void newIndicatorCreationVisibleViaFindByTicker(
            @ForAll("positiveDecimalValues") BigDecimal newValue
    ) {
        IndicatorType newType = IndicatorType.ROE;

        EntityManager em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();
        AssetJpaEntity entity = em.find(AssetJpaEntity.class, seededAssetId);

        entity.getIndicatorValues().removeIf(iv -> iv.getId().getIndicatorType().equals(newType.code()));
        em.flush();

        AssetIndicatorValueEntity newIv = new AssetIndicatorValueEntity();
        newIv.setId(new AssetIndicatorValueId(seededAssetId, newType.code()));
        newIv.setAsset(entity);
        newIv.setValue(newValue);
        entity.getIndicatorValues().add(newIv);
        em.flush();
        em.getTransaction().commit();
        em.close();

        Optional<AssetJpaEntity> fetched = springDataRepository.findByTicker("AAPL");
        assertThat(fetched).isPresent();

        Optional<AssetIndicatorValueEntity> createdIndicator = fetched.get().getIndicatorValues().stream()
                .filter(iv -> iv.getId().getIndicatorType().equals(newType.code()))
                .findFirst();
        assertThat(createdIndicator)
                .as("New indicator %s should be visible via findByTicker", newType)
                .isPresent();
        assertThat(createdIndicator.get().getValue())
                .as("New indicator %s value should match", newType)
                .isEqualByComparingTo(newValue);
    }

    /**
     * Property 2c: Existing Indicator Update Preservation
     *
     * When an existing indicator value is updated (via direct entity manipulation
     * simulating what save() does internally), the updated value is correctly
     * persisted and visible via findByTicker().
     *
     * This tests the update path using findByTicker which works on unfixed code.
     *
     * Validates: Requirements 3.3
     */
    @Property(tries = 10)
    void existingIndicatorUpdateVisibleViaFindByTicker(
            @ForAll("positiveDecimalValues") BigDecimal updatedValue
    ) {
        EntityManager em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();
        AssetJpaEntity entity = em.find(AssetJpaEntity.class, seededAssetId);

        entity.getIndicatorValues().stream()
                .filter(iv -> iv.getId().getIndicatorType().equals(IndicatorType.PRICE.code()))
                .findFirst()
                .ifPresent(iv -> iv.setValue(updatedValue));
        em.flush();
        em.getTransaction().commit();
        em.close();

        Optional<AssetJpaEntity> fetched = springDataRepository.findByTicker("AAPL");
        assertThat(fetched).isPresent();

        Optional<AssetIndicatorValueEntity> priceIndicator = fetched.get().getIndicatorValues().stream()
                .filter(iv -> iv.getId().getIndicatorType().equals(IndicatorType.PRICE.code()))
                .findFirst();
        assertThat(priceIndicator)
                .as("PRICE indicator should be present after update")
                .isPresent();
        assertThat(priceIndicator.get().getValue())
                .as("PRICE indicator value should be updated to %s", updatedValue)
                .isEqualByComparingTo(updatedValue);
    }

    /**
     * Property 2d: Missing Asset Preservation
     *
     * For any non-existent asset id, calling save() throws IllegalArgumentException.
     * This path does NOT trigger the LazyInitializationException because findById()
     * returns empty, causing the orElseThrow() to fire before reaching indicatorValues.
     *
     * Validates: Requirements 3.4
     */
    @Property(tries = 10)
    void saveWithNonExistentIdThrowsIllegalArgumentException(
            @ForAll("nonExistentIds") Long nonExistentId
    ) {
        Map<IndicatorType, BigDecimal> indicators = new EnumMap<>(IndicatorType.class);
        indicators.put(IndicatorType.PRICE, new BigDecimal("100.00"));

        Asset asset = new Asset(nonExistentId, "INVALID", indicators);

        assertThatThrownBy(() -> assetRepositoryAdapter.save(asset))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Asset not found with id: " + nonExistentId);
    }

    private void updateIndicatorsViaEntityManager(Map<IndicatorType, BigDecimal> indicators) {
        EntityManager em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();
        AssetJpaEntity entity = em.find(AssetJpaEntity.class, seededAssetId);

        for (Map.Entry<IndicatorType, BigDecimal> entry : indicators.entrySet()) {
            String code = entry.getKey().code();
            BigDecimal value = entry.getValue();

            entity.getIndicatorValues().stream()
                    .filter(iv -> iv.getId().getIndicatorType().equals(code))
                    .findFirst()
                    .ifPresentOrElse(
                            iv -> iv.setValue(value),
                            () -> {
                                AssetIndicatorValueEntity newIv = new AssetIndicatorValueEntity();
                                newIv.setId(new AssetIndicatorValueId(seededAssetId, code));
                                newIv.setAsset(entity);
                                newIv.setValue(value);
                                entity.getIndicatorValues().add(newIv);
                            }
                    );
        }

        em.flush();
        em.getTransaction().commit();
        em.close();
    }

    @Provide
    Arbitrary<Map<IndicatorType, BigDecimal>> randomIndicatorValues() {
        return Arbitraries.of(IndicatorType.values())
                .list()
                .ofMinSize(1)
                .ofMaxSize(IndicatorType.values().length)
                .uniqueElements()
                .flatMap(types -> {
                    Arbitrary<BigDecimal> values = Arbitraries.doubles()
                            .between(0.01, 9999.99)
                            .ofScale(4)
                            .map(BigDecimal::valueOf);

                    return values.list().ofSize(types.size()).map(valueList -> {
                        Map<IndicatorType, BigDecimal> map = new EnumMap<>(IndicatorType.class);
                        for (int i = 0; i < types.size(); i++) {
                            map.put(types.get(i), valueList.get(i));
                        }
                        return map;
                    });
                });
    }

    @Provide
    Arbitrary<BigDecimal> positiveDecimalValues() {
        return Arbitraries.doubles()
                .between(0.01, 9999.99)
                .ofScale(4)
                .map(BigDecimal::valueOf);
    }

    @Provide
    Arbitrary<Long> nonExistentIds() {
        return Arbitraries.longs().between(9000L, 99999L);
    }
}
