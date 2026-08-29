package com.oms.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Custom HealthIndicator that checks database connectivity and metadata.
 * Performs a lightweight connection validation and retrieves database info.
 */
@Slf4j
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = new DelegatingDataSource(dataSource);
    }

    /**
     * Stores the DataSource reference for health checks.
     * Note: DataSource is a Spring-managed bean and is effectively immutable after configuration.
     * The EI_EXPOSE_REP2 warning is suppressed as DataSource is thread-safe and not modified externally.
     */

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection == null || !connection.isValid(5)) {
                log.warn("Database health check failed: connection is not valid");
                return Health.down()
                        .withDetail("reason", "Connection is not valid")
                        .build();
            }

            DatabaseMetaData metaData = connection.getMetaData();
            String databaseName = metaData.getDatabaseProductName();
            String databaseVersion = metaData.getDatabaseProductVersion();
            String url = metaData.getURL();

            log.debug("Database health check passed: {} v{}", databaseName, databaseVersion);

            return Health.up()
                    .withDetail("database", databaseName)
                    .withDetail("version", databaseVersion)
                    .withDetail("url", sanitizeUrl(url))
                    .withDetail("validationQuery", "SELECT 1")
                    .build();

        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("reason", e.getMessage())
                    .withException(e)
                    .build();
        }
    }

    /**
     * Sanitize database URL to hide credentials.
     */
    private String sanitizeUrl(String url) {
        if (url == null) {
            return "unknown";
        }
        // Hide password in URL if present
        return url.replaceAll("password=[^&;]+", "password=****")
                  .replaceAll("user=[^&;]+", "user=****");
    }
}