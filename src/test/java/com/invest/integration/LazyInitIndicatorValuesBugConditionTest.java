package com.invest.integration;

import com.invest.domain.entities.Asset;
import com.invest.domain.entities.enumerator.IndicatorType;
import com.invest.infrastructure.persistence.SpringDataAssetRepository;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bug condition exploration test for LazyInitializationException in AssetRepositoryAdapter.save().
 *
 * Root cause: AssetRepositoryAdapter.save() loads an entity via findById() which does NOT
 * eagerly fetch the indicatorValues collection (mapped with FetchType.LAZY). When save()
 * subsequently accesses entity.getIndicatorValues().stream(), a LazyInitializationException
 * is thrown because there is no active Hibernate session.
 *
 * This test reproduces the bug by:
 * 1. Inserting an asset with existing indicator values into the database
 * 2. Calling AssetRepositoryAdapter.save(Asset(id, ticker, indicators)) with a valid id
 * 3. Asserting that no exception is thrown AND the returned asset has the expected indicator values
 *
 * EXPECTED OUTCOME: Test FAILS with LazyInitializationException (this proves the bug exists).
 *
 * Validates: Requirements 1.1, 1.2
 */
class LazyInitIndicatorValuesBugConditionTest {

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
     * Property 1: Bug Condition - LazyInitializationException on Save with FindById
     *
     * For all assets with valid ids in the database and any subset of IndicatorType
     * with corresponding BigDecimal values, AssetRepositoryAdapter.save() completes
     * without exception and returns an asset with the expected indicator values.
     *
     * EXPECTED TO FAIL on unfixed code with LazyInitializationException at
     * entity.getIndicatorValues().stream()
     *
     * Validates: Requirements 1.1, 1.2
     */
    @Property(tries = 10)
    void saveWithValidIdShouldNotThrowLazyInitializationException(
            @ForAll("indicatorSubsets") Map<IndicatorType, BigDecimal> indicators
    ) {
        Asset domainAsset = new Asset(seededAssetId, "AAPL", indicators);

        Asset result = assetRepositoryAdapter.save(domainAsset);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(seededAssetId);
        assertThat(result.getTicker()).isEqualTo("AAPL");

        for (Map.Entry<IndicatorType, BigDecimal> entry : indicators.entrySet()) {
            assertThat(result.getIndicator(entry.getKey()))
                    .as("Indicator %s should have value %s", entry.getKey(), entry.getValue())
                    .isEqualByComparingTo(entry.getValue());
        }
    }

    @Provide
    Arbitrary<Map<IndicatorType, BigDecimal>> indicatorSubsets() {
        Arbitrary<List<IndicatorType>> types = Arbitraries.of(IndicatorType.values())
                .list()
                .ofMinSize(1)
                .ofMaxSize(IndicatorType.values().length)
                .uniqueElements();

        return types.flatMap(typeList -> {
            Arbitrary<BigDecimal> values = Arbitraries.doubles()
                    .between(0.01, 9999.99)
                    .ofScale(4)
                    .map(BigDecimal::valueOf);

            return values.list().ofSize(typeList.size()).map(valueList -> {
                Map<IndicatorType, BigDecimal> map = new EnumMap<>(IndicatorType.class);
                for (int i = 0; i < typeList.size(); i++) {
                    map.put(typeList.get(i), valueList.get(i));
                }
                return map;
            });
        });
    }
}
