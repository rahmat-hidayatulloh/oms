package com.oms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Configuration for custom Micrometer business metrics.
 * 
 * <p>
 * Provides custom counters and timers for OMS business operations:
 * <ul>
 * <li>Order creation counter (by status)</li>
 * <li>Order processing timer</li>
 * <li>Product cache hit/miss counter</li>
 * <li>Authentication attempts counter (by outcome)</li>
 * </ul>
 * 
 * <p>
 * These metrics are exported via Prometheus endpoint at /actuator/prometheus.
 */
@Configuration
public class MetricsConfig {

    /**
     * Counter for order creation attempts.
     * Tags: status (created, failed, out_of_stock)
     */
    @Bean
    public Counter orderCreatedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("oms.orders.created")
                .description("Number of orders created")
                .tag("status", "created")
                .register(meterRegistry);
    }

    /**
     * Counter for failed order creation attempts.
     */
    @Bean
    public Counter orderFailedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("oms.orders.failed")
                .description("Number of failed order creation attempts")
                .tag("status", "failed")
                .register(meterRegistry);
    }

    /**
     * Timer for order processing duration.
     */
    @Bean
    public Timer orderProcessingTimer(MeterRegistry meterRegistry) {
        return Timer.builder("oms.orders.processing.time")
                .description("Time taken to process order creation")
                .register(meterRegistry);
    }

    /**
     * Counter for product cache hits.
     */
    @Bean
    public Counter productCacheHitCounter(MeterRegistry meterRegistry) {
        return Counter.builder("oms.cache.product.hits")
                .description("Number of product cache hits")
                .register(meterRegistry);
    }

    /**
     * Counter for product cache misses.
     */
    @Bean
    public Counter productCacheMissCounter(MeterRegistry meterRegistry) {
        return Counter.builder("oms.cache.product.misses")
                .description("Number of product cache misses")
                .register(meterRegistry);
    }

    /**
     * Counter for authentication attempts.
     * Tags: outcome (success, failure, rate_limited)
     */
    @Bean
    public Counter authAttemptsCounter(MeterRegistry meterRegistry) {
        return Counter.builder("oms.auth.attempts")
                .description("Number of authentication attempts")
                .tag("outcome", "success")
                .register(meterRegistry);
    }

    /**
     * Counter for failed authentication attempts.
     */
    @Bean
    public Counter authFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("oms.auth.failures")
                .description("Number of failed authentication attempts")
                .tag("outcome", "failure")
                .register(meterRegistry);
    }
}