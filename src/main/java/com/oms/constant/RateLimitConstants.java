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
    // NOTE: the RFC 7807 "type" URI for 429 is no longer a constant. It is
    // environment-aware and derived from `app.error.base-url`
    // (ERROR_BASE_URL) by RateLimitResponseBuilder via ProblemDetailFactory,
    // so dev/staging/prod each advertise their own error base URI
    // (standard 4.1 — externalize configuration).
}
