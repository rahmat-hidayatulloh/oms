package com.oms.config;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/**
 * Enables caching support for the application.
 * <p>
 * The cache backend is chosen by configuration: Redis when
 * {@code spring.data.redis.enabled=true} (staging/prod/dev-with-redis), the
 * in-memory provider otherwise. Both are wired by Spring Boot / {@link RedisConfig}.
 * <p>
 * Exposes {@link ResilientCacheErrorHandler} so a cache outage degrades to
 * database reads instead of failing the request with HTTP 500
 * (standard 4.2 — graceful degradation).
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new ResilientCacheErrorHandler();
    }
}