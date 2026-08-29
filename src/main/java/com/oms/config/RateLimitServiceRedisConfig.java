package com.oms.config;

import com.oms.ratelimit.RateLimitService;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

/**
 * Activates Redis-backed distributed rate limiting when both
 * {@code app.rate-limit.distributed=true} and
 * {@code spring.data.redis.enabled=true}.
 * <p>
 * When this configuration is active, {@link RateLimitService} stores its
 * Bucket4j buckets in Redis so that token counters are shared across
 * all application pods. When disabled, {@link RateLimitService} falls back
 * to in-memory {@code ConcurrentHashMap} buckets (per-instance limiting).
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.rate-limit.distributed", havingValue = "true")
public class RateLimitServiceRedisConfig {

    private final RateLimitService rateLimitService;
    private final RedisConnectionFactory connectionFactory;

    public RateLimitServiceRedisConfig(RateLimitService rateLimitService,
                                       RedisConnectionFactory connectionFactory) {
        this.rateLimitService = rateLimitService;
        this.connectionFactory = connectionFactory;
    }

    @PostConstruct
    void init() {
        if (!(connectionFactory instanceof LettuceConnectionFactory lettuceFactory)) {
            log.warn("Distributed rate limiting requires LettuceConnectionFactory; "
                    + "falling back to in-memory mode");
            return;
        }
        try {
            AbstractRedisClient client = lettuceFactory.getRequiredNativeClient();
            if (!(client instanceof RedisClient redisClient)) {
                log.warn("Distributed rate limiting requires a standalone RedisClient; "
                        + "falling back to in-memory mode");
                return;
            }

            ProxyManager<byte[]> proxyManager = LettuceBasedProxyManager
                    .builderFor(redisClient)
                    .withExpirationStrategy(
                            ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                                    Duration.ofMinutes(30)))
                    .build();

            rateLimitService.initDistributedMode(proxyManager);
            log.info("Redis-backed distributed rate limiting initialized successfully");
        } catch (Exception e) {
            log.warn("Failed to initialize distributed rate limiting; "
                    + "falling back to in-memory mode: {}", e.getMessage());
        }
    }
}