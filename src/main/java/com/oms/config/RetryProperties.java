package com.oms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for retry mechanism.
 * Bound from {@code app.retry.*} in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "app.retry")
public class RetryProperties {

    private int maxAttempts = 3;
    private long backoffDelayMs = 100;
    private double backoffMultiplier = 2.0;
    private long maxBackoffDelayMs = 5000;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getBackoffDelayMs() {
        return backoffDelayMs;
    }

    public void setBackoffDelayMs(long backoffDelayMs) {
        this.backoffDelayMs = backoffDelayMs;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public void setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }

    public long getMaxBackoffDelayMs() {
        return maxBackoffDelayMs;
    }

    public void setMaxBackoffDelayMs(long maxBackoffDelayMs) {
        this.maxBackoffDelayMs = maxBackoffDelayMs;
    }
}