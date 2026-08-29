package com.oms.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;

/**
 * Tests error paths in {@link ExternalAPIHealthIndicator} using Mockito.
 */
@ExtendWith(MockitoExtension.class)
class ExternalAPIHealthIndicatorErrorTest {

    private final CircuitBreakerRegistry cbRegistry =
        CircuitBreakerRegistry.ofDefaults();
    private final RetryRegistry retryRegistry =
        RetryRegistry.of(RetryConfig.custom()
            .maxAttempts(1).build());

    @Test
    void health_shouldReturnUp_whenHttp200() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        org.mockito.Mockito.lenient()
            .when(response.statusCode()).thenReturn(200);
        org.mockito.Mockito.lenient()
            .when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)))
            .thenReturn(response);

        ExternalAPIHealthIndicator indicator =
            new ExternalAPIHealthIndicator(
                "http://localhost:9090/health",
                java.time.Duration.ofSeconds(5),
                cbRegistry, retryRegistry);
        setField(indicator, "httpClient", httpClient);

        Health health = indicator.health();

        assertThat(health.getStatus().getCode())
            .isEqualTo("UP");
        assertThat(health.getDetails())
            .containsEntry("httpStatus", 200)
            .containsEntry("service",
                "External Payment API")
            .containsEntry("responseTime", "simulated");
    }

    @Test
    void health_shouldReturnDown_whenHttp500()
            throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        org.mockito.Mockito.lenient()
            .when(response.statusCode()).thenReturn(500);
        org.mockito.Mockito.lenient()
            .when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)))
            .thenReturn(response);

        ExternalAPIHealthIndicator indicator =
            new ExternalAPIHealthIndicator(
                "http://localhost:9090/health",
                java.time.Duration.ofSeconds(5),
                cbRegistry, retryRegistry);
        setField(indicator, "httpClient", httpClient);

        Health health = indicator.health();

        assertThat(health.getStatus().getCode())
            .isEqualTo("DOWN");
        assertThat(health.getDetails())
            .containsEntry("httpStatus", 500)
            .containsKey("reason");
    }

    @Test
    void health_shouldReturnDown_whenHttp404()
            throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        org.mockito.Mockito.lenient()
            .when(response.statusCode()).thenReturn(404);
        org.mockito.Mockito.lenient()
            .when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)))
            .thenReturn(response);

        ExternalAPIHealthIndicator indicator =
            new ExternalAPIHealthIndicator(
                "http://localhost:9090/health",
                java.time.Duration.ofSeconds(5),
                cbRegistry, retryRegistry);
        setField(indicator, "httpClient", httpClient);

        Health health = indicator.health();

        assertThat(health.getStatus().getCode())
            .isEqualTo("DOWN");
        assertThat(health.getDetails())
            .containsEntry("httpStatus", 404)
            .containsKey("reason");
    }

    @Test
    void health_shouldFallback_whenConnectionRefused() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        org.mockito.Mockito.lenient()
            .when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)))
            .thenThrow(new java.net.ConnectException(
                "Connection refused"));

        ExternalAPIHealthIndicator indicator =
            new ExternalAPIHealthIndicator(
                "http://localhost:9090/health",
                java.time.Duration.ofSeconds(5),
                cbRegistry, retryRegistry);
        setField(indicator, "httpClient", httpClient);

        Health health = indicator.health();

        assertThat(health.getStatus().getCode())
            .isEqualTo("DOWN");
        assertThat(health.getDetails())
            .containsKey("reason")
            .containsEntry("circuitBreaker", "OPEN");
    }

    @Test
    void health_shouldFallback_whenTimeout() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        org.mockito.Mockito.lenient()
            .when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)))
            .thenThrow(
                new java.net.http.HttpTimeoutException(
                    "Request timed out"));

        ExternalAPIHealthIndicator indicator =
            new ExternalAPIHealthIndicator(
                "http://localhost:9090/health",
                java.time.Duration.ofSeconds(5),
                cbRegistry, retryRegistry);
        setField(indicator, "httpClient", httpClient);

        Health health = indicator.health();

        assertThat(health.getStatus().getCode())
            .isEqualTo("DOWN");
        assertThat(health.getDetails())
            .containsKey("reason")
            .containsEntry("circuitBreaker", "OPEN");
    }

    @Test
    void health_shouldFallback_whenGenericException() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        org.mockito.Mockito.lenient()
            .when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)))
            .thenThrow(new RuntimeException(
                "Unexpected error"));

        ExternalAPIHealthIndicator indicator =
            new ExternalAPIHealthIndicator(
                "http://localhost:9090/health",
                java.time.Duration.ofSeconds(5),
                cbRegistry, retryRegistry);
        setField(indicator, "httpClient", httpClient);

        Health health = indicator.health();

        assertThat(health.getStatus().getCode())
            .isEqualTo("DOWN");
        assertThat(health.getDetails())
            .containsKey("reason")
            .containsEntry("circuitBreaker", "OPEN");
    }

    @Test
    void health_shouldContainUrlInDetails()
            throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        org.mockito.Mockito.lenient()
            .when(response.statusCode()).thenReturn(200);
        org.mockito.Mockito.lenient()
            .when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)))
            .thenReturn(response);

        ExternalAPIHealthIndicator indicator =
            new ExternalAPIHealthIndicator(
                "http://localhost:9090/health",
                java.time.Duration.ofSeconds(5),
                cbRegistry, retryRegistry);
        setField(indicator, "httpClient", httpClient);

        Health health = indicator.health();

        assertThat(health.getDetails())
            .containsEntry("url",
                "http://localhost:9090/health");
    }

    private void setField(Object obj, String fieldName,
                          Object value) {
        try {
            Field field =
                obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
