package com.oms.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Custom HealthIndicator that checks Redis connectivity via PING command.
 * Only active when {@code spring.data.redis.enabled} is true.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public Health health() {
        RedisConnection connection = null;
        try {
            connection = redisConnectionFactory.getConnection();
            
            String pingResponse = connection.ping();
            boolean isHealthy = "PONG".equalsIgnoreCase(pingResponse);

            if (isHealthy) {
                log.debug("Redis health check passed: PONG");
                return Health.up()
                        .withDetail("service", "Redis")
                        .withDetail("ping", pingResponse)
                        .build();
            } else {
                log.warn("Redis health check failed: unexpected PING response '{}'", pingResponse);
                return Health.down()
                        .withDetail("service", "Redis")
                        .withDetail("ping", pingResponse)
                        .withDetail("reason", "Unexpected PING response")
                        .build();
            }

        } catch (Exception e) {
            log.error("Redis health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("service", "Redis")
                    .withDetail("reason", e.getMessage())
                    .withException(e)
                    .build();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    log.warn("Failed to close Redis connection: {}", e.getMessage());
                }
            }
        }
    }
}