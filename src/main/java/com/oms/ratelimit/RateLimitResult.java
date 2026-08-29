package com.oms.ratelimit;

/**
 * Result of a rate limit check: whether the request is allowed plus the bucket
 * state reported back to the client via {@code X-RateLimit-*} headers.
 */
public record RateLimitResult(
        boolean allowed,
        long remainingTokens,
        long limit,
        long resetSeconds
) {
    /** Convenience factory for an allowed result at full capacity. */
    public static RateLimitResult allowed(long limit, long resetSeconds) {
        return new RateLimitResult(true, limit, limit, resetSeconds);
    }
}
