package com.oms.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that Flyway migrations run on startup and produce the expected schema
 * and seed data. Uses the in-memory H2 test datasource configured in application-test.yml.
 */
@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allMigrationTablesAreCreated() {
        assertTrue(tableExists("users"), "users table should exist after migration");
        assertTrue(tableExists("products"), "products table should exist after migration");
        assertTrue(tableExists("orders"), "orders table should exist after migration");
        assertTrue(tableExists("order_items"), "order_items table should exist after migration");
    }

    @Test
    void adminUserIsSeeded() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ? AND role = ?",
                Integer.class, "admin", "ADMIN");
        assertEquals(1, count, "exactly one admin user should be seeded");
    }

    @Test
    void flywayHistoryRecordsAllMigrations() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE \"success\" = TRUE AND \"version\" != '0'",
                Integer.class);
        assertEquals(7, count, "all seven migrations should be recorded as successful");
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?",
                Integer.class, tableName.toUpperCase());
        return count != null && count > 0;
    }
}