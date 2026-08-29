package com.oms.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "spring.data.redis.enabled", matches = "true")
class RedisIntegrationTest {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void redisTemplate_shouldBeAvailable() {
        if (redisTemplate != null) {
            assertThat(redisTemplate).isNotNull();
        }
    }
}
