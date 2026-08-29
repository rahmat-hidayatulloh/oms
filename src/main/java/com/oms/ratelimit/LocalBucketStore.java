package com.oms.ratelimit;

import com.oms.ratelimit.RateLimitProperties.EndpointConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * In-memory (per-instance) rate-limit bucket store backed by a bounded
 * {@link ConcurrentHashMap}. Extracted from {@code RateLimitService} (V4-01):
 * owns bucket lifecycle, idle eviction, and the cleanup executor.
 * <p>
 * Idle buckets are evicted both periodically (scheduled sweep) and eagerly
 * (when the retained set crosses {@link #MAX_BUCKETS}) to bound memory growth.
 */
@Component
public class LocalBucketStore {

    private static final Logger log = LoggerFactory.getLogger(LocalBucketStore.class);

    /** Buckets idle longer than this are evicted to prevent unbounded memory growth. */
    private static final long IDLE_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(30);

    /** Soft upper bound on retained buckets; crossing it triggers an eager sweep. */
    private static final long MAX_BUCKETS = 50_000;

    private final BucketFactory bucketFactory;

    /** Buckets keyed by "endpointName:clientKey", bounded by idle eviction. */
    private final Map<String, BucketEntry> localBuckets = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleanupExecutor;

    public LocalBucketStore(BucketFactory bucketFactory) {
        this.bucketFactory = bucketFactory;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-cleanup");
            t.setDaemon(true);
            return t;
        });
    }

    @PostConstruct
    void init() {
        cleanupExecutor.scheduleAtFixedRate(this::purgeStaleBuckets, 5, 5, TimeUnit.MINUTES);
    }

    @PreDestroy
    void shutdown() {
        cleanupExecutor.shutdownNow();
    }

    /**
     * Attempts to consume a single token from the local bucket for the given
     * endpoint and client key.
     */
    public RateLimitResult tryConsume(String endpointName, String clientKey) {
        BucketEntry entry = resolveBucketEntry(endpointName, clientKey);
        ConsumptionProbe probe = entry.getBucket().tryConsumeAndReturnRemaining(1);

        boolean allowed = probe.isConsumed();
        long remaining = probe.getRemainingTokens();
        long reset = probe.getNanosToWaitForRefill() / 1_000_000_000;

        if (!allowed) {
            log.debug("Rate limit exceeded (local) for '{}' by client '{}'", endpointName, clientKey);
        }
        return new RateLimitResult(allowed, remaining, entry.getCapacity(), reset);
    }

    /** Gets or creates a bucket entry for the given endpoint and client key. */
    private BucketEntry resolveBucketEntry(String endpointName, String clientKey) {
        String cacheKey = endpointName + ":" + clientKey;
        BucketEntry entry = localBuckets.computeIfAbsent(cacheKey, key -> {
            EndpointConfig config = bucketFactory.resolveConfig(endpointName);
            log.debug("Created bucket for '{}' (capacity={}, refill={}/{})",
                    cacheKey, config.getCapacity(), config.getRefillTokens(), config.getRefillPeriod());
            return new BucketEntry(bucketFactory.newBucket(config), config.getCapacity());
        });
        entry.touch();

        // Bound the map: if the retained set grew beyond the soft cap, sweep idle
        // entries now rather than letting memory grow without limit.
        if (localBuckets.size() > MAX_BUCKETS) {
            purgeStaleBuckets();
        }
        return entry;
    }

    /**
     * Removes buckets that have been idle for longer than {@link #IDLE_TIMEOUT_NANOS}.
     * Package-private for testability.
     */
    void purgeStaleBuckets() {
        long now = System.nanoTime();
        localBuckets.entrySet().removeIf(e -> now - e.getValue().lastAccessNanos() > IDLE_TIMEOUT_NANOS);
        log.debug("Rate-limit bucket cleanup: {} buckets retained", localBuckets.size());
    }

    /**
     * Holds a bucket, its configured capacity, and the last time it was accessed.
     * The last-access timestamp drives the idle-eviction sweep.
     */
    private static final class BucketEntry {

        private final Bucket bucket;
        private final long capacity;
        private volatile long lastAccessNanos;

        BucketEntry(Bucket bucket, long capacity) {
            this.bucket = bucket;
            this.capacity = capacity;
            this.lastAccessNanos = System.nanoTime();
        }

        Bucket getBucket() {
            return bucket;
        }

        long getCapacity() {
            return capacity;
        }

        long lastAccessNanos() {
            return lastAccessNanos;
        }

        void touch() {
            this.lastAccessNanos = System.nanoTime();
        }
    }
}
