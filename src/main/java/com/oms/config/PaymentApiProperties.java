package com.oms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Externalized configuration for the external payment API.
 * <p>
 * Binds {@code app.payment.api.*}. Before this class existed the health
 * indicator consumed the full URL through a single {@code @Value}
 * ({@code app.payment.api.health-url}), which forced every environment to
 * supply a complete URL. The URL is now composed from a base URL plus a
 * health path so that only the environment-specific part changes
 * (dev = {@code localhost:9090}, docker/minikube = {@code mock-payment:8080}).
 *
 * <pre>
 * app:
 *   payment:
 *     api:
 *       base-url: ${PAYMENT_API_BASE_URL:http://localhost:9090}
 *       health-path: ${PAYMENT_API_HEALTH_PATH:/health}
 *       health-url: ${HEALTH_URL:}          # legacy full-URL override
 *   health-check:
 *     timeout: ${HEALTH_CHECK_TIMEOUT:5s}
 * </pre>
 *
 * The legacy {@code health-url} key is still honoured (non-blank wins) so that
 * existing deployments passing {@code HEALTH_URL}
 * ({@code docker-compose.yml}, {@code scripts/deploy.sh}, minikube ConfigMap)
 * keep working without changes.
 */
@ConfigurationProperties(prefix = "app.payment.api")
public class PaymentApiProperties {

    /**
     * Environment-specific base URL of the external payment API
     * ({@code scheme://host:port}, no trailing path).
     */
    private String baseUrl = "http://localhost:9090";

    /**
     * Path of the health endpoint, appended to {@link #baseUrl}.
     */
    private String healthPath = "/health";

    /**
     * Legacy full health URL. Kept for backward compatibility — when set it
     * takes precedence over {@link #baseUrl} + {@link #healthPath}.
     */
    private String healthUrl = "";

    /**
     * Resolves the full health-check URL.
     * <p>
     * Precedence: {@link #healthUrl} (legacy {@code HEALTH_URL}) &rarr;
     * {@code baseUrl + healthPath}. Trailing/leading slashes are normalised so
     * {@code http://host/} + {@code /health} does not yield {@code //health}.
     *
     * @return the absolute health URL consumed by {@code ExternalAPIHealthIndicator}
     */
    public String getHealthUrl() {
        if (StringUtils.hasText(healthUrl)) {
            return healthUrl.trim();
        }
        String base = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "";
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String path = StringUtils.hasText(healthPath) ? healthPath.trim() : "";
        if (!path.isEmpty() && !path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    public void setHealthUrl(String healthUrl) {
        this.healthUrl = healthUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getHealthPath() {
        return healthPath;
    }

    public void setHealthPath(String healthPath) {
        this.healthPath = healthPath;
    }
}