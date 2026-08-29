package com.oms.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized CORS configuration properties.
 * <p>
 * Allows per-environment CORS origin configuration via
 * {@code application-{profile}.yml} instead of hardcoding origins
 * in {@link SecurityConfig}. This satisfies 12-Factor App (F-12),
 * environment-aware CORS (SOLID O-2), and SRP by reducing
 * SecurityConfig responsibilities (SOLID S-3).
 *
 * <pre>
 * app:
 *   cors:
 *     allowed-origins:
 *       - http://localhost:3000
 *     allowed-methods:
 *       - GET
 *       - POST
 *     allowed-headers:
 *       - "*"
 *     exposed-headers:
 *       - Authorization
 *     allow-credentials: true
 * </pre>
 */
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of("http://localhost:3000");

    private List<String> allowedMethods = List.of(
        "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private List<String> allowedHeaders = List.of("*");

    private List<String> exposedHeaders = List.of(
        "Authorization", "X-API-Deprecated",
        "X-API-Deprecation-Replacement", "Sunset");

    private boolean allowCredentials = true;

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public List<String> getExposedHeaders() {
        return exposedHeaders;
    }

    public void setExposedHeaders(List<String> exposedHeaders) {
        this.exposedHeaders = exposedHeaders;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }
}
