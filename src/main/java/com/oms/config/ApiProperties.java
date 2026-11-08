package com.oms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.api.base-url} — the public base URL of the OMS API for the
 * current environment.
 * <p>
 * This is the single place where the environment-aware API base URL is
 * declared, so related configuration (currently: the {@code contact} URL shown
 * in the OpenAPI spec) can be set once per deployment instead of being
 * hardcoded or inferred from the environment name (standard 4.1 — externalize
 * configuration; 4.5 — no environment detection).
 *
 * <pre>
 * app:
 *   api:
 *     base-url: ${API_BASE_URL:http://localhost:8080}
 * </pre>
 */
@ConfigurationProperties(prefix = "app.api")
public class ApiProperties {

    public static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private String baseUrl = DEFAULT_BASE_URL;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}