package com.oms.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for per-endpoint rate limiting.
 * Binds to {@code app.rate-limit.*} in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    /** Whether rate limiting is enabled globally. */
    private boolean enabled = true;

    /** Whether distributed (Redis-backed) rate limiting is enabled.
     *  When true and Redis is available, buckets are stored in Redis for
     *  consistency across pods. When false, falls back to in-memory per-instance.
     */
    private boolean distributed = false;

    /** List of trusted proxy IP addresses or CIDR notations.
     *  X-Forwarded-For header is only trusted from these proxies.
     */
    private String[] trustedProxies = {};

    /** Default rate limit settings applied to all endpoints. */
    private EndpointConfig defaults = new EndpointConfig();

    /** Per-endpoint overrides keyed by endpoint name. */
    private Map<String, EndpointConfig> endpoints = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDistributed() {
        return distributed;
    }

    public void setDistributed(boolean distributed) {
        this.distributed = distributed;
    }

    public String[] getTrustedProxies() {
        return trustedProxies;
    }

    public void setTrustedProxies(String[] trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    public EndpointConfig getDefaults() {
        return defaults;
    }

    public void setDefaults(EndpointConfig defaults) {
        this.defaults = defaults;
    }

    public Map<String, EndpointConfig> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, EndpointConfig> endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * Configuration for a single endpoint or the default group.
     */
    public static class EndpointConfig {

        /** Maximum number of requests allowed in the refill period. */
        private int capacity = 10;

        /** Number of tokens added per refill period. */
        private int refillTokens = 10;

        /** Duration of the refill window. */
        private Duration refillPeriod = Duration.ofMinutes(1);

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public int getRefillTokens() {
            return refillTokens;
        }

        public void setRefillTokens(int refillTokens) {
            this.refillTokens = refillTokens;
        }

        public Duration getRefillPeriod() {
            return refillPeriod;
        }

        public void setRefillPeriod(Duration refillPeriod) {
            this.refillPeriod = refillPeriod;
        }
    }
}