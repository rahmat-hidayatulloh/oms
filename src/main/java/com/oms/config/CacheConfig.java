package com.oms.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables caching support for the application.
 * When Redis is not available, Spring falls back to ConcurrentMapCacheManager.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}