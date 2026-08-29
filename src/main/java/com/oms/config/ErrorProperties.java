package com.oms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized error configuration properties (RFC 7807).
 * <p>
 * Binds {@code app.error.base-url} — the base URI used to build the
 * {@code type} field of ProblemDetail responses. Previously this value was
 * hardcoded in {@code BaseExceptionHandler} while the unbound
 * {@code app.error.base-url} key sat unused in {@code application.yml}.
 * Externalizing it makes error type URIs environment-aware (12-Factor).
 *
 * <pre>
 * app:
 *   error:
 *     base-url: ${ERROR_BASE_URL:https://api.oms.com/errors}
 * </pre>
 */
@ConfigurationProperties(prefix = "app.error")
public class ErrorProperties {

    /**
     * Default base URL for RFC 7807 error type URIs (documentation site).
     */
    public static final String DEFAULT_BASE_URL = "https://api.oms.com/errors";

    private String baseUrl = DEFAULT_BASE_URL;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}