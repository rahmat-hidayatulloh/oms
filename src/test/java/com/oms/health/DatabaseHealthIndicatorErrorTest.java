package com.oms.health;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests error paths in {@link DatabaseHealthIndicator}.
 */
class DatabaseHealthIndicatorErrorTest {

    @Test
    void health_shouldReturnDown_whenConnectionFails() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection refused"));

        DatabaseHealthIndicator indicator = new DatabaseHealthIndicator(dataSource);

        var health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
    }

    @Test
    void health_shouldReturnDown_whenConnectionIsNotValid() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(connection.isValid(5)).thenReturn(false);
        when(dataSource.getConnection()).thenReturn(connection);

        DatabaseHealthIndicator indicator = new DatabaseHealthIndicator(dataSource);

        var health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
        assertThat(health.getDetails()).containsKey("reason");
    }

    @Test
    void health_shouldReturnDown_withReason() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection refused"));

        DatabaseHealthIndicator indicator = new DatabaseHealthIndicator(dataSource);

        var health = indicator.health();

        assertThat(health.getDetails()).containsKey("reason");
    }
}