package com.oms.constant;

/**
 * Rate limiting constants for Bucket4j integration.
 */
public final class RateLimitConstants {

    private RateLimitConstants() {
    }

    public static final String MSG_RATE_LIMIT_EXCEEDED = "Too many requests. Please try again later.";
    public static final String HEADER_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";
    public static final String RATE_LIMIT_ERROR_URL = "https://api.oms.com/errors/429";
}
