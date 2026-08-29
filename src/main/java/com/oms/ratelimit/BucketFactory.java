package com.oms.ratelimit;

import com.oms.ratelimit.RateLimitProperties.EndpointConfig;


import org.springframework.stereotype.Component;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;

/**
 * Builds Bucket4j buckets/configurations and resolves the effective
 * {@link EndpointConfig} for a logical endpoint name. Extracted from
 * {@code RateLimitService} (V4-01) so bucket construction has a single home,
 * shared by both the in-memory and Redis-backed stores.
 */
@Component
public class BucketFactory {

    private final RateLimitProperties properties;

    public BucketFactory(RateLimitProperties properties) {
        this.properties = properties;
    }

    /**
     * Resolves the configuration for an endpoint, falling back to the defaults
     * group when the endpoint has no specific override.
     */
    public EndpointConfig resolveConfig(String endpointName) {
        if (endpointName != null && properties.getEndpoints().containsKey(endpointName)) {
            return properties.getEndpoints().get(endpointName);
        }
        return properties.getDefaults();
    }

    /** Creates a new local {@link Bucket} for the given endpoint configuration. */
    public Bucket newBucket(EndpointConfig config) {
        return Bucket.builder().addLimit(bandwidth(config)).build();
    }

    /** Creates a distributed {@link BucketConfiguration} for the given endpoint configuration. */
    public BucketConfiguration newBucketConfiguration(EndpointConfig config) {
        return BucketConfiguration.builder().addLimit(bandwidth(config)).build();
    }

    private Bandwidth bandwidth(EndpointConfig config) {
        return Bandwidth.classic(
                config.getCapacity(),
                Refill.greedy(config.getRefillTokens(), config.getRefillPeriod()));
    }
}
