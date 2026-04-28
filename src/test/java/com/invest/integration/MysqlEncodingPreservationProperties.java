package com.invest.integration;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import net.jqwik.api.constraints.CharRange;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.BeforeContainer;
import org.testcontainers.containers.MySQLContainer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 2: Preservation - ASCII and Numeric Data Unchanged
 *
 * Observation-first methodology: these tests observe behavior on UNFIXED code for
 * non-buggy inputs (ASCII-only strings, numeric fields). The expected outcome is that
 * all tests PASS, confirming the baseline behavior to preserve after the fix.
 *
 * The MySQL container is started WITHOUT MYSQL_CHARACTER_SET_SERVER env var (same as
 * the bug condition test), and both the buggy connection (SET NAMES latin1) and the
 * fixed connection (characterEncoding=UTF-8) are used in the same test to compare.
 *
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5
 */
class MysqlEncodingPreservationProperties {

    @SuppressWarnings("resource")
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    private static String baseUrl;

    @BeforeContainer
    static void startContainer() throws Exception {
        mysql.start();
        baseUrl = String.format("jdbc:mysql://%s:%d/testdb", mysql.getHost(), mysql.getMappedPort(3306));

        try (Connection conn = DriverManager.getConnection(baseUrl, "testuser", "testpass");
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS preservation_text (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255)
                    ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS preservation_numeric (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        current_price DECIMAL(10,2),
                        dividend_yield DECIMAL(10,2),
                        p_vp DECIMAL(10,2)
                    ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
        }
    }

    @AfterContainer
    static void stopContainer() {
        mysql.stop();
    }

    /**
     * Writes a string via the given connection and reads it back.
     * Each call inserts a new row to avoid cross-test interference.
     */
    private String writeAndRead(Connection conn, String value) throws Exception {
        int id;
        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO preservation_text (name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, value);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                keys.next();
                id = keys.getInt(1);
            }
        }
        try (PreparedStatement select = conn.prepareStatement(
                "SELECT name FROM preservation_text WHERE id = ?")) {
            select.setInt(1, id);
            try (ResultSet rs = select.executeQuery()) {
                rs.next();
                return rs.getString("name");
            }
        }
    }

    /**
     * Writes decimal fields via the given connection and reads them back as a BigDecimal[3].
     */
    private BigDecimal[] writeAndReadDecimals(Connection conn,
                                              BigDecimal price,
                                              BigDecimal dividendYield,
                                              BigDecimal pvp) throws Exception {
        int id;
        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO preservation_numeric (current_price, dividend_yield, p_vp) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            insert.setBigDecimal(1, price);
            insert.setBigDecimal(2, dividendYield);
            insert.setBigDecimal(3, pvp);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                keys.next();
                id = keys.getInt(1);
            }
        }
        try (PreparedStatement select = conn.prepareStatement(
                "SELECT current_price, dividend_yield, p_vp FROM preservation_numeric WHERE id = ?")) {
            select.setInt(1, id);
            try (ResultSet rs = select.executeQuery()) {
                rs.next();
                return new BigDecimal[]{
                        rs.getBigDecimal("current_price"),
                        rs.getBigDecimal("dividend_yield"),
                        rs.getBigDecimal("p_vp")
                };
            }
        }
    }

    /**
     * Property 2a: ASCII string preservation.
     *
     * For any ASCII-only string, write_and_read(buggyConnection, text) must equal
     * write_and_read(fixedConnection, text), and both must equal the original text.
     *
     * Observation: ASCII bytes are identical in both latin1 and UTF-8, so the encoding
     * mismatch does not corrupt them. This confirms the baseline to preserve after the fix.
     *
     * Validates: Requirements 3.1, 3.4
     */
    @Property(tries = 100)
    @Tag("Feature: mysql-encoding-fix, Property 2: Preservation")
    void asciiStringPreservation(
            @ForAll @StringLength(min = 1, max = 100) @CharRange(from = 'a', to = 'z') String lowerAscii
    ) throws Exception {
        try (Connection buggyConn = DriverManager.getConnection(baseUrl, "testuser", "testpass");
             Connection fixedConn = DriverManager.getConnection(
                     baseUrl + "?characterEncoding=UTF-8&useUnicode=true", "testuser", "testpass")) {

            try (Statement stmt = buggyConn.createStatement()) {
                stmt.execute("SET NAMES latin1");
            }

            String buggyResult = writeAndRead(buggyConn, lowerAscii);
            String fixedResult = writeAndRead(fixedConn, lowerAscii);

            assertThat(buggyResult)
                    .as("Buggy connection must preserve ASCII string [%s]", lowerAscii)
                    .isEqualTo(lowerAscii);
            assertThat(fixedResult)
                    .as("Fixed connection must preserve ASCII string [%s]", lowerAscii)
                    .isEqualTo(lowerAscii);
            assertThat(buggyResult)
                    .as("Buggy and fixed connections must produce identical results for ASCII [%s]", lowerAscii)
                    .isEqualTo(fixedResult);
        }
    }

    /**
     * Property 2b: ASCII uppercase string preservation.
     *
     * Same as 2a but for uppercase ASCII, covering domain names like
     * "Maxi Renda", "XP Malls", "VALORA HEDGE FUND".
     *
     * Validates: Requirements 3.1, 3.4
     */
    @Property(tries = 100)
    @Tag("Feature: mysql-encoding-fix, Property 2: Preservation")
    void asciiUppercaseStringPreservation(
            @ForAll @StringLength(min = 1, max = 100) @CharRange(from = 'A', to = 'Z') String upperAscii
    ) throws Exception {
        try (Connection buggyConn = DriverManager.getConnection(baseUrl, "testuser", "testpass");
             Connection fixedConn = DriverManager.getConnection(
                     baseUrl + "?characterEncoding=UTF-8&useUnicode=true", "testuser", "testpass")) {

            try (Statement stmt = buggyConn.createStatement()) {
                stmt.execute("SET NAMES latin1");
            }

            String buggyResult = writeAndRead(buggyConn, upperAscii);
            String fixedResult = writeAndRead(fixedConn, upperAscii);

            assertThat(buggyResult)
                    .as("Buggy connection must preserve uppercase ASCII string [%s]", upperAscii)
                    .isEqualTo(upperAscii);
            assertThat(fixedResult)
                    .as("Fixed connection must preserve uppercase ASCII string [%s]", upperAscii)
                    .isEqualTo(upperAscii);
            assertThat(buggyResult)
                    .as("Buggy and fixed connections must produce identical results for uppercase ASCII [%s]", upperAscii)
                    .isEqualTo(fixedResult);
        }
    }

    /**
     * Property 2c: Numeric field preservation.
     *
     * For any valid DECIMAL(10,2) values representing current_price, dividend_yield,
     * and p_vp, both the buggy and fixed connections must store and retrieve the exact
     * same values. Numeric fields are unaffected by charset encoding.
     *
     * Validates: Requirements 3.2, 3.4
     */
    @Property(tries = 100)
    @Tag("Feature: mysql-encoding-fix, Property 2: Preservation")
    void numericFieldPreservation(
            @ForAll("validPrices") BigDecimal price,
            @ForAll("validYields") BigDecimal dividendYield,
            @ForAll("validPvp") BigDecimal pvp
    ) throws Exception {
        try (Connection buggyConn = DriverManager.getConnection(baseUrl, "testuser", "testpass");
             Connection fixedConn = DriverManager.getConnection(
                     baseUrl + "?characterEncoding=UTF-8&useUnicode=true", "testuser", "testpass")) {

            try (Statement stmt = buggyConn.createStatement()) {
                stmt.execute("SET NAMES latin1");
            }

            BigDecimal[] buggyResult = writeAndReadDecimals(buggyConn, price, dividendYield, pvp);
            BigDecimal[] fixedResult = writeAndReadDecimals(fixedConn, price, dividendYield, pvp);

            assertThat(buggyResult[0].compareTo(price))
                    .as("Buggy connection must preserve current_price [%s]", price)
                    .isZero();
            assertThat(buggyResult[1].compareTo(dividendYield))
                    .as("Buggy connection must preserve dividend_yield [%s]", dividendYield)
                    .isZero();
            assertThat(buggyResult[2].compareTo(pvp))
                    .as("Buggy connection must preserve p_vp [%s]", pvp)
                    .isZero();

            assertThat(fixedResult[0].compareTo(price))
                    .as("Fixed connection must preserve current_price [%s]", price)
                    .isZero();
            assertThat(fixedResult[1].compareTo(dividendYield))
                    .as("Fixed connection must preserve dividend_yield [%s]", dividendYield)
                    .isZero();
            assertThat(fixedResult[2].compareTo(pvp))
                    .as("Fixed connection must preserve p_vp [%s]", pvp)
                    .isZero();

            assertThat(buggyResult[0].compareTo(fixedResult[0]))
                    .as("Buggy and fixed connections must produce identical current_price")
                    .isZero();
            assertThat(buggyResult[1].compareTo(fixedResult[1]))
                    .as("Buggy and fixed connections must produce identical dividend_yield")
                    .isZero();
            assertThat(buggyResult[2].compareTo(fixedResult[2]))
                    .as("Buggy and fixed connections must produce identical p_vp")
                    .isZero();
        }
    }

    @Provide
    Arbitrary<BigDecimal> validPrices() {
        return Arbitraries.doubles()
                .between(0.01, 9999999.99)
                .ofScale(2)
                .map(BigDecimal::valueOf);
    }

    @Provide
    Arbitrary<BigDecimal> validYields() {
        return Arbitraries.doubles()
                .between(0.00, 99.99)
                .ofScale(2)
                .map(BigDecimal::valueOf);
    }

    @Provide
    Arbitrary<BigDecimal> validPvp() {
        return Arbitraries.doubles()
                .between(0.00, 99.99)
                .ofScale(2)
                .map(BigDecimal::valueOf);
    }
}
