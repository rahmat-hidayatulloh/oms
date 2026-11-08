package com.oms.config;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Cache error handler that degrades gracefully when the cache backend (Redis)
 * is unavailable.
 * <p>
 * Spring's default ({@code SimpleCacheErrorHandler}) rethrows every cache
 * failure, so a Redis outage turns cached endpoints ({@code GET /products},
 * {@code GET /products/{id}}) into HTTP 500 even though the database can serve
 * the request. This handler logs the failure at WARN and lets the call fall
 * through to the database instead (standard 4.2 — graceful degradation).
 * <p>
 * Applicable to the {@code @Cacheable}/{@code @CachePut}/{@code @CacheEvict}
 * layer only. Redis is still required for refresh-token storage and the
 * distributed rate limiter, which must fail closed for security reasons.
 */
@Slf4j
public class ResilientCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        logCacheError("GET", exception, cache, key);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        logCacheError("PUT", exception, cache, key);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        logCacheError("EVICT", exception, cache, key);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        logCacheError("CLEAR", exception, cache, null);
    }

    private void logCacheError(String operation, RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache {} failed for cache '{}' key '{}' ({}). Continuing without cache — "
                        + "the request is served from the database.",
                operation, cache == null ? "unknown" : cache.getName(), key,
                exception.getMessage());
        log.debug("Cache error detail", exception);
    }
}