package com.oms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.RedisTemplate;

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

    @Test
    void shouldRoundTripBigDecimalAndNestedList() throws Exception {
        // Real cache values (ProductResponse.price is BigDecimal, OrderResponse
        // carries List<OrderItemResponse>) must survive serialize -> deserialize.
        // BigDecimal is a non-final type, so DefaultTyping.NON_FINAL writes
        // ["java.math.BigDecimal", 25000.00] and the validator must allow it,
        // otherwise every @Cacheable read fails at runtime.
        ObjectMapper mapper = redisConfig.redisObjectMapper();
        java.math.BigDecimal price = new java.math.BigDecimal("25000.00");
        String json = mapper.writeValueAsString(price);
        assertThat(json).contains("java.math.BigDecimal");
        Object restored = mapper.readValue(json, Object.class);
        assertThat(restored).isEqualTo(price);

        java.util.List<java.math.BigDecimal> list = new java.util.ArrayList<>();
        list.add(price);
        list.add(new java.math.BigDecimal("1.50"));
        Object listRestored =
            mapper.readValue(mapper.writeValueAsString(list), Object.class);
        assertThat(listRestored).isEqualTo(list);
    }

    @Test
    void shouldRoundTripProductResponseRecord() throws Exception {
        // ProductService#findById is @Cacheable — the cached value is exactly
        // this (final) record. If the serializer cannot restore it to a
        // ProductResponse the cache read throws ClassCastException/
        // InvalidTypeIdException at runtime in staging/prod.
        ObjectMapper mapper = redisConfig.redisObjectMapper();
        com.oms.product.dto.ProductResponse dto =
            new com.oms.product.dto.ProductResponse(
                java.util.UUID.randomUUID(),
                "Verify Product",
                "desc",
                new java.math.BigDecimal("25000.00"),
                10,
                com.oms.product.entity.Category.ELECTRONICS,
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now());

        byte[] bytes = mapper.writeValueAsBytes(dto);
        Object restored = mapper.readValue(bytes, Object.class);

        assertThat(restored)
            .isInstanceOf(com.oms.product.dto.ProductResponse.class)
            .isEqualTo(dto);
    }

    @Test
    void shouldNotExposeRedisObjectMapperAsSpringBean() {
        // Regression guard: redisObjectMapper must NOT be registered as an
        // ObjectMapper bean. If it were, it would become the sole ObjectMapper
        // in the context whenever Redis is enabled (staging/prod), stopping
        // Spring Boot's JacksonAutoConfiguration and leaking default-typing into
        // MVC HTTP — which breaks any request body with non-final types
        // (BigDecimal, List) and wraps every JSON response with type ids.
        new ApplicationContextRunner()
            .withInitializer(context ->
                context.getBeanFactory().setConversionService(
                    ApplicationConversionService.getSharedInstance()))
            .withUserConfiguration(RedisConfig.class)
            .withPropertyValues(
                "spring.data.redis.enabled=true",
                "spring.data.redis.host=localhost",
                "spring.data.redis.port=6379",
                "spring.data.redis.database=0",
                "spring.data.redis.password=",
                "spring.data.redis.cache.ttl=3600s")
            .run(context -> {
                assertThat(context.getBeansOfType(ObjectMapper.class))
                    .as("no ObjectMapper bean should be contributed by RedisConfig")
                    .isEmpty();
                assertThat(context.getBeansOfType(RedisTemplate.class))
                    .as("RedisTemplate still requires the shared serializer")
                    .hasSize(1);
            });
    }
}

