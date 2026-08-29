package com.oms.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j Circuit Breaker configuration for external payment API.
 * <p>
 * Prevents cascading failures when the payment API is unavailable by
 * short-circuiting requests after a configurable failure threshold.
 * Circuit breaker state is visible via Actuator
 * ({@code /actuator/circuitbreakers}).
 * <p>
 * Primary configuration is externalized in {@code application.yml}
 * under {@code resilience4j.circuitbreaker.instances.paymentApi}.
 * This class provides the programmatic fallback defaults and ensures
 * the {@link CircuitBreakerRegistry} bean is created.
 */
@Configuration
public class CircuitBreakerConfig {

    /**
     * Creates the shared {@link CircuitBreakerRegistry} with sensible defaults.
     * YAML properties override these defaults at runtime.
     *
     * @return configured CircuitBreakerRegistry
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig defaultConfig =
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(100)
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(
                    java.net.ConnectException.class,
                    java.net.http.HttpTimeoutException.class,
                    java.io.IOException.class,
                    java.util.concurrent.TimeoutException.class
                )
                .build();

        return CircuitBreakerRegistry.of(defaultConfig);
    }

    /**
     * Creates the shared {@link io.github.resilience4j.retry.RetryRegistry}
     * with sensible defaults. YAML properties override these defaults at runtime.
     *
     * @return configured RetryRegistry
     */
    @Bean
    public RetryRegistry retryRegistry() {
        io.github.resilience4j.retry.RetryConfig defaultConfig =
            io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(1000))
                .retryExceptions(
                    java.net.ConnectException.class,
                    java.net.http.HttpTimeoutException.class,
                    java.io.IOException.class
                )
                .build();

        return RetryRegistry.of(defaultConfig);
    }
}
