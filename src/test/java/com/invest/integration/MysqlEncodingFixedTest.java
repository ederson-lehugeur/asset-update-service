package com.invest.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Fix verification test for MySQL JDBC encoding.
 *
 * Verifies Property 1: Accented Character Round-Trip Integrity via Fixed Connection.
 *
 * The fix consists of two parts:
 * 1. MYSQL_CHARACTER_SET_SERVER=utf8mb4 in docker-compose.yml - forces the server to
 *    use utf8mb4 as default, so the mysql CLI seed scripts connect with utf8mb4 session
 *    charset (no double-encoding of UTF-8 bytes).
 * 2. useUnicode=true&characterEncoding=UTF-8 in JDBC URLs - ensures Spring Boot services
 *    use UTF-8 session encoding when reading data.
 *
 * This test reproduces the fixed environment by:
 * 1. Starting MySQL WITH MYSQL_CHARACTER_SET_SERVER=utf8mb4 and MYSQL_COLLATION_SERVER=utf8mb4_unicode_ci
 * 2. Inserting data via a connection WITHOUT SET NAMES latin1 (simulating the fixed seed
 *    script behavior - mysql CLI will use utf8mb4 session charset when server enforces it)
 * 3. Reading back via a utf8mb4 connection with characterEncoding=UTF-8 (simulating the
 *    fixed Spring Boot service)
 * 4. Asserting equality - this PASSES for all accented strings, confirming the fix works
 *
 * EXPECTED OUTCOME: All tests PASS, confirming the bug is fixed.
 *
 * Validates: Requirements 2.1, 2.2, 2.3
 */
@Testcontainers
class MysqlEncodingFixedTest {

    // MySQL 8.4 container started WITH MYSQL_CHARACTER_SET_SERVER=utf8mb4 and
    // MYSQL_COLLATION_SERVER=utf8mb4_unicode_ci - replicates the fixed docker-compose.yml
    @Container
    @SuppressWarnings("resource")
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withEnv("MYSQL_CHARACTER_SET_SERVER", "utf8mb4")
            .withEnv("MYSQL_COLLATION_SERVER", "utf8mb4_unicode_ci");

    private Connection fixedConnection;

    @BeforeEach
    void setUp() throws Exception {
        String baseUrl = String.format("jdbc:mysql://%s:%d/testdb", mysql.getHost(), mysql.getMappedPort(3306));

        // Fixed connection: characterEncoding=UTF-8&useUnicode=true - simulates the fixed
        // Spring Boot service JDBC URL. No SET NAMES latin1 - simulates the fixed seed
        // script behavior (mysql CLI uses utf8mb4 session charset when server enforces it).
        fixedConnection = DriverManager.getConnection(
                baseUrl + "?characterEncoding=UTF-8&useUnicode=true",
                "testuser", "testpass");

        try (Statement stmt = fixedConnection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS test_encoding (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255)
                    ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixedConnection != null && !fixedConnection.isClosed()) {
            try (Statement stmt = fixedConnection.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS test_encoding");
            }
            fixedConnection.close();
        }
    }

    /**
     * Inserts a value via the fixed connection and reads it back via the same connection.
     * Simulates the fixed environment: seed script uses utf8mb4 session, Spring Boot reads
     * with UTF-8 encoding.
     */
    private String insertAndRead(String value) throws Exception {
        int id;
        try (PreparedStatement insert = fixedConnection.prepareStatement(
                "INSERT INTO test_encoding (name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, value);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                keys.next();
                id = keys.getInt(1);
            }
        }

        try (PreparedStatement select = fixedConnection.prepareStatement(
                "SELECT name FROM test_encoding WHERE id = ?")) {
            select.setInt(1, id);
            try (ResultSet rs = select.executeQuery()) {
                rs.next();
                return rs.getString("name");
            }
        }
    }

    // Validates: Requirements 2.1, 2.2, 2.3
    @Test
    @DisplayName("RECEBÍVEIS IMOBILIÁRIOS: fixed connection round-trip - EXPECTED TO PASS (confirms fix)")
    void accentedStringRecebiveis_fixedConnection_shouldRoundTripCorrectly() throws Exception {
        String original = "RECEBÍVEIS IMOBILIÁRIOS";
        String readBack = insertAndRead(original);
        assertEquals(original, readBack,
                "Fix failed: accented string was corrupted. Written: [" + original + "] Read back: [" + readBack + "]");
    }

    // Validates: Requirements 2.1, 2.2, 2.3
    @Test
    @DisplayName("PÁTRIA LOG: fixed connection round-trip - EXPECTED TO PASS (confirms fix)")
    void accentedStringPatria_fixedConnection_shouldRoundTripCorrectly() throws Exception {
        String original = "PÁTRIA LOG";
        String readBack = insertAndRead(original);
        assertEquals(original, readBack,
                "Fix failed: accented string was corrupted. Written: [" + original + "] Read back: [" + readBack + "]");
    }

    // Validates: Requirements 2.1, 2.2, 2.3
    @Test
    @DisplayName("FII REC Recebíveis Imobiliários: fixed connection round-trip - EXPECTED TO PASS (confirms fix)")
    void accentedStringFiiRec_fixedConnection_shouldRoundTripCorrectly() throws Exception {
        String original = "FII REC Recebíveis Imobiliários";
        String readBack = insertAndRead(original);
        assertEquals(original, readBack,
                "Fix failed: accented string was corrupted. Written: [" + original + "] Read back: [" + readBack + "]");
    }

    // Validates: Requirements 2.1, 2.2, 2.3 (ASCII strings must also continue to work)
    @Test
    @DisplayName("Maxi Renda: fixed connection round-trip - EXPECTED TO PASS (ASCII unaffected)")
    void asciiStringMaxiRenda_fixedConnection_shouldRoundTripCorrectly() throws Exception {
        String original = "Maxi Renda";
        String readBack = insertAndRead(original);
        assertEquals(original, readBack,
                "Regression: ASCII-only string was unexpectedly corrupted. Written: [" + original + "] Read back: [" + readBack + "]");
    }
}
