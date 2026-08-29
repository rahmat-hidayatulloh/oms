package com.oms.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link RedisHealthIndicator}.
 * Verifies Redis health check functionality using mocks.
 */
class RedisHealthIndicatorTest {

    @Test
    void health_shouldReturnUp_whenPingReturnsPong() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");

        RedisHealthIndicator indicator = new RedisHealthIndicator(connectionFactory);

        Health health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails()).containsEntry("service", "Redis");
        assertThat(health.getDetails()).containsEntry("ping", "PONG");
    }

    @Test
    void health_shouldReturnDown_whenPingReturnsUnexpected() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("UNEXPECTED");

        RedisHealthIndicator indicator = new RedisHealthIndicator(connectionFactory);

        Health health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
        assertThat(health.getDetails()).containsKey("reason");
    }

    @Test
    void health_shouldReturnDown_whenExceptionOccurs() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        when(connectionFactory.getConnection()).thenThrow(new RuntimeException("Connection refused"));

        RedisHealthIndicator indicator = new RedisHealthIndicator(connectionFactory);

        Health health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
        assertThat(health.getDetails()).containsKey("reason");
    }
}
