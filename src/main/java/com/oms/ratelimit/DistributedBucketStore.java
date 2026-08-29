package com.oms.ratelimit;

import com.oms.ratelimit.RateLimitProperties.EndpointConfig;

import java.nio.charset.StandardCharsets;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;

/**
 * Redis-backed (distributed) rate-limit bucket store. Extracted from
 * {@code RateLimitService} (V4-01). Buckets are stored in Redis via a
 * Bucket4j {@link ProxyManager} so token counters are shared across all pods.
 * <p>
 * The proxy manager is supplied at runtime by
 * {@link com.oms.config.RateLimitServiceRedisConfig} through
 * {@link #activate(ProxyManager)} when both distributed mode and Redis are
 * enabled; until then {@link #isActive()} returns {@code false} and the
 * service falls back to the {@link LocalBucketStore}.
 */
@Component
public class DistributedBucketStore {

    private static final Logger log = LoggerFactory.getLogger(DistributedBucketStore.class);

    private final BucketFactory bucketFactory;

    /** Non-null once distributed mode has been activated. */
    private ProxyManager<byte[]> proxyManager = null;

    public DistributedBucketStore(BucketFactory bucketFactory) {
        this.bucketFactory = bucketFactory;
    }

    /** Activates Redis-backed distributed rate limiting with the given proxy manager. */
    public void activate(ProxyManager<byte[]> proxyManager) {
        this.proxyManager = proxyManager;
        log.info("Rate limiting mode: DISTRIBUTED (Redis-backed)");
    }

    /** Whether Redis-backed distributed rate limiting is active. */
    public boolean isActive() {
        return proxyManager != null;
    }

    /**
     * Attempts to consume a single token from the distributed bucket for the
     * given endpoint and client key.
     */
    public RateLimitResult tryConsume(String endpointName, String clientKey) {
        byte[] cacheKey = (endpointName + ":" + clientKey).getBytes(StandardCharsets.UTF_8);
        EndpointConfig config = bucketFactory.resolveConfig(endpointName);

        ConsumptionProbe probe = proxyManager.builder()
                .build(cacheKey, () -> bucketFactory.newBucketConfiguration(config))
                .tryConsumeAndReturnRemaining(1);

        boolean allowed = probe.isConsumed();
        long remaining = probe.getRemainingTokens();
        long limit = config.getCapacity();
        long reset = probe.getNanosToWaitForRefill() / 1_000_000_000;

        if (!allowed) {
            log.debug("Rate limit exceeded (distributed) for '{}' by client '{}'", endpointName, clientKey);
        }
        return new RateLimitResult(allowed, remaining, limit, reset);
    }
}
