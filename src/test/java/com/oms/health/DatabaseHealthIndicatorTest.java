package com.oms.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link DatabaseHealthIndicator}.
 * Verifies database health check functionality.
 */
@SpringBootTest
@ActiveProfiles("test")
class DatabaseHealthIndicatorTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DatabaseHealthIndicator databaseHealthIndicator;

    @Test
    void health_shouldReturnUp_whenDatabaseIsAvailable() {
        var health = databaseHealthIndicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails()).containsKey("database");
        assertThat(health.getDetails()).containsKey("version");
        assertThat(health.getDetails()).containsKey("url");
    }

    @Test
    void health_shouldContainDatabaseDetails() {
        var health = databaseHealthIndicator.health();

        assertThat(health.getDetails().get("database")).isNotNull();
        assertThat(health.getDetails().get("version")).isNotNull();
        assertThat(health.getDetails().get("url")).isNotNull();
    }

    @Test
    void health_shouldSanitizeUrl() {
        var health = databaseHealthIndicator.health();
        String url = (String) health.getDetails().get("url");

        assertThat(url).doesNotContain("password=");
        if (url != null && url.contains("?")) {
            assertThat(url).matches(".*password=\\*{4}.*");
        }
    }

    @Test
    void sanitizeUrl_shouldHidePassword() {
        String urlWithPassword = "jdbc:postgresql://localhost:5432/testdb?password=secret123&user=admin";

        String sanitized = invokeSanitizeUrl(urlWithPassword);

        assertThat(sanitized).contains("password=****");
        assertThat(sanitized).doesNotContain("secret123");
    }

    @Test
    void sanitizeUrl_shouldHideUser() {
        String urlWithUser = "jdbc:postgresql://localhost:5432/testdb?user=admin&password=secret";

        String sanitized = invokeSanitizeUrl(urlWithUser);

        assertThat(sanitized).contains("user=****");
        assertThat(sanitized).doesNotContain("admin");
    }

    @Test
    void sanitizeUrl_shouldReturnUnknown_whenUrlIsNull() {
        String sanitized = invokeSanitizeUrl(null);

        assertThat(sanitized).isEqualTo("unknown");
    }

    @Test
    void dataSource_shouldBeInjected() {
        assertThat(dataSource).isNotNull();
    }

    @Test
    void databaseConnection_shouldBeValid() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5);

            assertThat(isValid).isTrue();
        }
    }

    @Test
    void databaseMetaData_shouldBeAccessible() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            assertThat(metaData).isNotNull();
            assertThat(metaData.getDatabaseProductName()).isNotNull();
            assertThat(metaData.getDatabaseProductVersion()).isNotNull();
        }
    }

    /**
     * Helper method to invoke private sanitizeUrl method via reflection.
     */
    private String invokeSanitizeUrl(String url) {
        try {
            java.lang.reflect.Method method = DatabaseHealthIndicator.class
                    .getDeclaredMethod("sanitizeUrl", String.class);
            method.setAccessible(true);
            return (String) method.invoke(databaseHealthIndicator, url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke sanitizeUrl", e);
        }
    }
}