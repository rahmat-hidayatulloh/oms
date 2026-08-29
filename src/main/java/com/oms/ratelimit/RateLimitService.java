package com.oms.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.annotation.PostConstruct;

/**
 * Facade that routes rate-limit checks to the appropriate bucket store.
 * <p>
 * <b>Strategy:</b>
 * <ul>
 *   <li>When distributed mode is active (Redis-backed), delegates to
 *       {@link DistributedBucketStore} — counters shared across all pods.</li>
 *   <li>Otherwise, delegates to {@link LocalBucketStore} — per-instance
 *       in-memory rate limiting.</li>
 * </ul>
 * <p>
 * Bucket construction lives in {@link BucketFactory}; the two stores own their
 * respective lifecycles. This class only decides which store handles a request
 * (V4-01: extracted from the former monolithic {@code RateLimitService}).
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    /**
     * Sentinel limit used when rate limiting is disabled so that the response
     * header reports an unlimited capacity instead of a magic {@link Long#MAX_VALUE}.
     */
    private static final long NO_LIMIT = Long.MAX_VALUE;

    private final RateLimitProperties properties;
    private final LocalBucketStore localStore;
    private final DistributedBucketStore distributedStore;

    public RateLimitService(RateLimitProperties properties,
                            LocalBucketStore localStore,
                            DistributedBucketStore distributedStore) {
        this.properties = properties;
        this.localStore = localStore;
        this.distributedStore = distributedStore;
    }

    @PostConstruct
    void logConfig() {
        log.info("Rate limiting enabled: {}", properties.isEnabled());
        if (properties.isEnabled()) {
            log.info("Rate limit defaults: capacity={}, refillTokens={}, refillPeriod={}",
                    properties.getDefaults().getCapacity(),
                    properties.getDefaults().getRefillTokens(),
                    properties.getDefaults().getRefillPeriod());
            properties.getEndpoints().forEach((name, config) ->
                    log.info("Rate limit endpoint '{}': capacity={}, refillTokens={}, refillPeriod={}",
                            name, config.getCapacity(), config.getRefillTokens(), config.getRefillPeriod()));
        }
    }

    /**
     * Initializes the Redis-backed proxy manager. Called from
     * {@link com.oms.config.RateLimitServiceRedisConfig} when both
     * distributed mode and Redis are enabled.
     */
    public void initDistributedMode(ProxyManager<byte[]> proxyManager) {
        distributedStore.activate(proxyManager);
    }

    /**
     * Attempts to consume a single token from the bucket for the given
     * endpoint and client key.
     *
     * @param endpointName the logical endpoint name (from @RateLimit or config)
     * @param clientKey    the client identifier (IP or userId)
     * @return result containing whether the request is allowed and bucket state
     */
    public RateLimitResult tryConsume(String endpointName, String clientKey) {
        if (!properties.isEnabled()) {
            return RateLimitResult.allowed(NO_LIMIT, 0);
        }
        if (distributedStore.isActive()) {
            return distributedStore.tryConsume(endpointName, clientKey);
        }
        return localStore.tryConsume(endpointName, clientKey);
    }

    /** Returns whether the service is operating in distributed mode. */
    public boolean isDistributedMode() {
        return distributedStore.isActive();
    }
}
