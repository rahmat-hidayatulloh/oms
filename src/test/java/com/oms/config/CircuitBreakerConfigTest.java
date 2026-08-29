package com.oms.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CircuitBreakerConfig}.
 * Verifies that the Resilience4j CircuitBreakerRegistry is properly
 * configured and the paymentApi instance is available.
 */
@SpringBootTest
@ActiveProfiles("test")
class CircuitBreakerConfigTest {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void circuitBreakerRegistry_shouldBeAvailable() {
        assertThat(circuitBreakerRegistry).isNotNull();
    }

    @Test
    void paymentApiCircuitBreaker_shouldBeConfigured() {
        var circuitBreaker =
            circuitBreakerRegistry.circuitBreaker("paymentApi");
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getName()).isEqualTo("paymentApi");
    }

    @Test
    void paymentApiCircuitBreaker_shouldHaveCorrectDefaults() {
        var circuitBreaker =
            circuitBreakerRegistry.circuitBreaker("paymentApi");
        var config = circuitBreaker.getCircuitBreakerConfig();

        assertThat(config.getFailureRateThreshold()).isEqualTo(50.0f);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState())
            .isEqualTo(3);
        assertThat(config.getSlidingWindowSize()).isEqualTo(10);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(5);
    }
}
