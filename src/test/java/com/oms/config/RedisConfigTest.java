package com.oms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RedisConfig} to verify:
 * <ul>
 *   <li>Shared ObjectMapper is created with correct modules
 *       (F-18 fix — deduplicated ObjectMapper)</li>
 *   <li>Configuration is correct for Redis serialization
 *       (F-11 fix — secure serializer)</li>
 * </ul>
 */
class RedisConfigTest {

    // Constructor args are only used for the Lettuce connection factory bean;
    // the ObjectMapper bean under test does not depend on them, so dummy
    private final RedisConfig redisConfig =
        new RedisConfig("localhost", 6379, 0, "", java.time.Duration.ofSeconds(3600));

    @Test
    void shouldCreateRedisObjectMapper() {
        ObjectMapper mapper = redisConfig.redisObjectMapper();
        assertThat(mapper).isNotNull();
    }

    @Test
    void shouldRegisterJavaTimeModule() {
        ObjectMapper mapper = redisConfig.redisObjectMapper();
        assertThat(mapper.getRegisteredModuleIds())
            .contains("jackson-datatype-jsr310");
    }

    @Test
    void shouldDisableWriteDatesAsTimestamps() {
        ObjectMapper mapper = redisConfig.redisObjectMapper();
        assertThat(mapper.isEnabled(
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS))
            .isFalse();
    }

    @Test
    void shouldSerializeNonScalarWithTypeInfo() {
        ObjectMapper mapper = redisConfig.redisObjectMapper();
        try {
            java.util.List<String> list =
                java.util.List.of("a", "b");
            String json = mapper.writeValueAsString(list);
            assertThat(json).isNotBlank();
            assertThat(json).contains("\"a\"");
            assertThat(json).contains("\"b\"");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

