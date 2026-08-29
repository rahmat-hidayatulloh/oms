package com.oms.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Custom HealthIndicator that checks external payment API availability.
 * <p>
 * Resilience4j Circuit Breaker and Retry prevent cascading failures
 * (F-13). Uses programmatic decoration because
 * {@link HealthIndicator#health()} cannot throw checked exceptions,
 * which is required for annotation-based Resilience4j AOP.
 */
@Slf4j
@Component
public class ExternalAPIHealthIndicator implements HealthIndicator {

    private static final String INSTANCE_NAME = "paymentApi";

    private final String paymentApiHealthUrl;
    private final Duration healthCheckTimeout;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    /**
     * HTTP client for health checks — lazily initialized to avoid
     * requiring Spring-managed HttpClient bean.
     */
    private HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // V4-02: constructor injection instead of field-level @Value.
    public ExternalAPIHealthIndicator(
            @Value("${app.payment.api.health-url:http://localhost:9090/health}") String paymentApiHealthUrl,
            @Value("${app.health-check.timeout:5s}") Duration healthCheckTimeout,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this.paymentApiHealthUrl = paymentApiHealthUrl;
        this.healthCheckTimeout = healthCheckTimeout;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    @Override
    public Health health() {
        CircuitBreaker cb =
            circuitBreakerRegistry.circuitBreaker(INSTANCE_NAME);
        Retry retry = retryRegistry.retry(INSTANCE_NAME);

        Supplier<Health> decorated =
            CircuitBreaker.decorateSupplier(cb, this::doHealthCheck);
        decorated = Retry.decorateSupplier(retry, decorated::get);

        try {
            return decorated.get();
        } catch (Exception e) {
            return fallbackHealth(e);
        }
    }

    private Health doHealthCheck() {
        try {
            HttpResponse<String> response =
                sendHealthCheckRequest();
            return buildHealthResponse(response);
        } catch (java.net.ConnectException e) {
            throw new RuntimeException(
                "Connection refused: " + e.getMessage(), e);
        } catch (java.net.http.HttpTimeoutException e) {
            throw new RuntimeException(
                "Request timed out: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException(
                "Health check failed: " + e.getMessage(), e);
        }
    }

    private Health fallbackHealth(Exception e) {
        log.warn("Payment API health check: fallback - {}",
                e.getMessage());
        return Health.down()
                .withDetail("service", "External Payment API")
                .withDetail("url", paymentApiHealthUrl)
                .withDetail("httpStatus", 0)
                .withDetail("responseTime", "N/A")
                .withDetail("reason",
                    "Circuit breaker fallback: " + e.getMessage())
                .withDetail("circuitBreaker", "OPEN")
                .build();
    }

    private HttpResponse<String> sendHealthCheckRequest()
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(paymentApiHealthUrl))
                .timeout(healthCheckTimeout)
                .GET()
                .build();
        return httpClient.send(
            request, HttpResponse.BodyHandlers.ofString());
    }

    private Health buildHealthResponse(
            HttpResponse<String> response) {
        int statusCode = response.statusCode();
        if (statusCode == 200) {
            log.debug("External API health check passed: HTTP {}",
                    statusCode);
            return Health.up()
                    .withDetail("service",
                        "External Payment API")
                    .withDetail("url", paymentApiHealthUrl)
                    .withDetail("httpStatus", statusCode)
                    .withDetail("responseTime", "simulated")
                    .build();
        }
        log.warn("External API health check failed: HTTP {}",
                statusCode);
        return Health.down()
                .withDetail("service",
                    "External Payment API")
                .withDetail("url", paymentApiHealthUrl)
                .withDetail("httpStatus", statusCode)
                .withDetail("reason",
                    "Unexpected HTTP status: " + statusCode)
                .build();
    }
}
