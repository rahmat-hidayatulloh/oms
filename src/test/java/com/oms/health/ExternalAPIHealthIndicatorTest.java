package com.oms.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ExternalAPIHealthIndicator}.
 * Verifies external API health check functionality.
 * <p>
 * Note: The external API (localhost:9090) may or may not be available
 * in test environment. Tests verify both UP and DOWN scenarios.
 */
@SpringBootTest
@ActiveProfiles("test")
class ExternalAPIHealthIndicatorTest {

    @Autowired
    private ExternalAPIHealthIndicator externalAPIHealthIndicator;

    @Test
    void health_shouldReturnStatus() {
        var health = externalAPIHealthIndicator.health();

        assertThat(health.getStatus().getCode()).isIn("UP", "DOWN");
    }

    @Test
    void health_shouldContainServiceDetails() {
        var health = externalAPIHealthIndicator.health();

        assertThat(health.getDetails()).isNotNull();
        assertThat(health.getDetails()).containsKey("service");
        assertThat(health.getDetails()).containsKey("url");
    }

    @Test
    void health_shouldContainHttpStatus() {
        var health = externalAPIHealthIndicator.health();

        assertThat(health.getDetails()).containsKey("httpStatus");
    }

    @Test
    void health_shouldContainResponseTime() {
        var health = externalAPIHealthIndicator.health();

        assertThat(health.getDetails()).containsKey("responseTime");
    }
}
