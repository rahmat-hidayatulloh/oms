package com.oms.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method or class for rate limiting.
 * <p>
 * When applied to a class, the limit applies to all methods in that class.
 * Method-level annotations override class-level settings.
 * <p>
 * The endpoint name is used to look up per-endpoint configuration from
 * {@code application.yml}. If no matching configuration is found, the
 * default rate limit values are used.
 * <p>
 * Usage:
 * <pre>
 * &#64;RateLimit(name = "auth-login", capacity = 5, refillTokens = 5, refillPeriodSeconds = 60)
 * &#64;PostMapping("/login")
 * public ResponseEntity&lt;LoginResponse&gt; login(...) { ... }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Logical endpoint name used to look up per-endpoint configuration
     * from {@code app.rate-limit.endpoints} in application.yml.
     * <p>
     * Falls back to the default config if no matching endpoint is found.
     */
    String name() default "";

    /**
     * Maximum number of concurrent requests (burst capacity).
     * Used when no endpoint-specific config is found in application.yml.
     */
    int capacity() default 10;

    /**
     * Number of tokens to refill per period.
     * Used when no endpoint-specific config is found in application.yml.
     */
    int refillTokens() default 10;

    /**
     * Refill period in seconds.
     * Used when no endpoint-specific config is found in application.yml.
     */
    int refillPeriodSeconds() default 60;
}