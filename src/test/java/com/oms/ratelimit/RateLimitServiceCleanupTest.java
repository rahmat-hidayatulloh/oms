package com.oms.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Verifies the in-memory rate-limit bucket map is bounded: idle buckets are
 * evicted by the cleanup sweep, while recently active buckets are retained.
 * <p>
 * After V4-01 the bucket lifecycle lives in {@link LocalBucketStore}; this test
 * targets that store directly.
 */
class RateLimitServiceCleanupTest {

    private LocalBucketStore store;

    @AfterEach
    void tearDown() {
        if (store != null) {
            Object executor = ReflectionTestUtils.getField(store, "cleanupExecutor");
            if (executor instanceof ExecutorService es) {
                es.shutdownNow();
            }
        }
    }

    private LocalBucketStore newStore() {
        RateLimitProperties props = new RateLimitProperties();
        props.setEnabled(true);
        RateLimitProperties.EndpointConfig defaults = new RateLimitProperties.EndpointConfig();
        defaults.setCapacity(10);
        defaults.setRefillTokens(10);
        props.setDefaults(defaults);
        return new LocalBucketStore(new BucketFactory(props));
    }

    @Test
    void purgeStaleBuckets_shouldRemoveIdleBuckets() {
        store = newStore();
        store.tryConsume("auth", "ip:192.0.2.1");

        Map<?, ?> buckets = (Map<?, ?>) ReflectionTestUtils.getField(store, "localBuckets");
        assertThat(buckets).hasSize(1);

        Object entry = buckets.values().iterator().next();

        long now = System.nanoTime();
        long idleThreshold = TimeUnit.MINUTES.toNanos(30);
        ReflectionTestUtils.setField(entry, "lastAccessNanos", now - idleThreshold - 1_000_000_000L);

        store.purgeStaleBuckets();

        assertThat(buckets).isEmpty();
    }

    @Test
    void purgeStaleBuckets_shouldRetainActiveBuckets() {
        store = newStore();
        store.tryConsume("auth", "ip:192.0.2.2");

        store.purgeStaleBuckets();

        Map<?, ?> buckets = (Map<?, ?>) ReflectionTestUtils.getField(store, "localBuckets");
        assertThat(buckets).hasSize(1);
    }
}
