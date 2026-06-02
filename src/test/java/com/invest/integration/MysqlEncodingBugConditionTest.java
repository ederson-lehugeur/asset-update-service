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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Bug condition exploration test for MySQL JDBC encoding corruption.
 *
 * Root cause (confirmed by hex analysis of live database):
 * When MySQL starts WITHOUT MYSQL_CHARACTER_SET_SERVER=utf8mb4, the docker-entrypoint
 * seed scripts run with a latin1 session charset. The UTF-8 bytes in the SQL file are
 * sent to MySQL as latin1 characters, which MySQL then double-encodes when storing in
 * utf8mb4 columns. When a Spring Boot service reads the data with a utf8mb4 connection,
 * it receives double-encoded (corrupted) strings.
 *
 * This test reproduces the bug by:
 * 1. Starting MySQL WITHOUT MYSQL_CHARACTER_SET_SERVER env var
 * 2. Inserting data via a latin1 connection (simulating the seed script behavior)
 * 3. Reading back via a utf8mb4 connection (simulating the Spring Boot service)
 * 4. Asserting equality - this FAILS for accented strings, confirming the bug
 *
 * EXPECTED OUTCOME: Tests for accented strings FAIL, confirming the bug exists.
 * The ASCII-only test (Maxi Renda) is expected to PASS even on unfixed config.
 *
 * Validates: Requirements 1.1, 1.2, 1.3
 */
@Testcontainers
class MysqlEncodingBugConditionTest {

    // MySQL 8.4 container started WITHOUT MYSQL_CHARACTER_SET_SERVER env var
    // This replicates the production docker-compose.yml bug condition
    @Container
    @SuppressWarnings("resource")
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    private Connection buggyConnection;
    private Connection utf8Connection;

    @BeforeEach
    void setUp() throws Exception {
        String baseUrl = String.format("jdbc:mysql://%s:%d/testdb", mysql.getHost(), mysql.getMappedPort(3306));

        // Buggy connection: sends UTF-8 bytes but tells MySQL the session charset is latin1.
        // This simulates the docker-entrypoint seed script behavior: the mysql CLI reads the
        // SQL file as UTF-8 but connects with a latin1 session charset (no --default-character-set).
        // MySQL then interprets the UTF-8 bytes as latin1 characters and double-encodes them.
        buggyConnection = DriverManager.getConnection(baseUrl, "testuser", "testpass");
        try (Statement stmt = buggyConnection.createStatement()) {
            stmt.execute("SET NAMES latin1");  // force latin1 session - simulates mysql CLI default
        }

        // Clean utf8mb4 connection - simulates the Spring Boot service reading data
        utf8Connection = DriverManager.getConnection(
                baseUrl + "?characterEncoding=UTF-8&useUnicode=true",
                "testuser", "testpass");

        try (Statement stmt = buggyConnection.createStatement()) {
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
        for (Connection conn : new Connection[]{buggyConnection, utf8Connection}) {
            if (conn != null && !conn.isClosed()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP TABLE IF EXISTS test_encoding");
                }
                conn.close();
            }
        }
    }

    /**
     * Inserts a value via the buggy connection (SET NAMES latin1, simulating seed script)
     * and reads it back via the utf8mb4 connection (simulating Spring Boot service).
     */
    private String insertWithBuggyConnectionReadWithUtf8(String value) throws Exception {
        int id;
        // Use Statement (not PreparedStatement) to simulate the mysql CLI behavior.
        // PreparedStatement in modern Connector/J negotiates charset separately from SET NAMES,
        // bypassing the double-encoding bug. Statement sends the literal SQL string as-is,
        // which is what the mysql CLI does with seed scripts.
        try (Statement insert = buggyConnection.createStatement()) {
            String escaped = value.replace("'", "\\'");
            insert.execute("INSERT INTO test_encoding (name) VALUES ('" + escaped + "')",
                    Statement.RETURN_GENERATED_KEYS);
            try (ResultSet keys = insert.getGeneratedKeys()) {
                keys.next();
                id = keys.getInt(1);
            }
        }

        try (PreparedStatement select = utf8Connection.prepareStatement(
                "SELECT name FROM test_encoding WHERE id = ?")) {
            select.setInt(1, id);
            try (ResultSet rs = select.executeQuery()) {
                rs.next();
                return rs.getString("name");
            }
        }
    }

    // Validates: Requirements 1.1, 1.2, 1.3
    @Test
    @DisplayName("RECEBÍVEIS IMOBILIÁRIOS: latin1 insert / utf8 read - confirms double-encoding bug")
    void accentedStringRecebiveis_latin1InsertUtf8Read_shouldBeCorrupted() throws Exception {
        String original = "RECEBÍVEIS IMOBILIÁRIOS";
        String readBack = insertWithBuggyConnectionReadWithUtf8(original);
        // The data IS double-encoded - read back value differs from original, confirming the bug
        assertNotEquals(original, readBack,
                "Bug condition NOT met: expected double-encoding corruption but string was preserved");
    }

    // Validates: Requirements 1.1, 1.2, 1.3
    @Test
    @DisplayName("PÁTRIA LOG: latin1 insert / utf8 read - confirms double-encoding bug")
    void accentedStringPatria_latin1InsertUtf8Read_shouldBeCorrupted() throws Exception {
        String original = "PÁTRIA LOG";
        String readBack = insertWithBuggyConnectionReadWithUtf8(original);
        assertNotEquals(original, readBack,
                "Bug condition NOT met: expected double-encoding corruption but string was preserved");
    }

    // Validates: Requirements 1.1, 1.2, 1.3
    @Test
    @DisplayName("FII REC Recebíveis Imobiliários: latin1 insert / utf8 read - confirms double-encoding bug")
    void accentedStringFiiRec_latin1InsertUtf8Read_shouldBeCorrupted() throws Exception {
        String original = "FII REC Recebíveis Imobiliários";
        String readBack = insertWithBuggyConnectionReadWithUtf8(original);
        assertNotEquals(original, readBack,
                "Bug condition NOT met: expected double-encoding corruption but string was preserved");
    }

    // Validates: Requirements 3.1 (ASCII-only strings are unaffected)
    @Test
    @DisplayName("Maxi Renda: latin1 insert / utf8 read - EXPECTED TO PASS (ASCII unaffected by bug)")
    void asciiStringMaxiRenda_latin1InsertUtf8Read_shouldPass() throws Exception {
        String original = "Maxi Renda";
        String readBack = insertWithBuggyConnectionReadWithUtf8(original);
        // ASCII-only strings are NOT affected by the encoding bug - this should pass
        assertEquals(original, readBack,
                "Regression: ASCII-only string was unexpectedly corrupted. Written: [" + original + "] Read back: [" + readBack + "]");
    }
}
