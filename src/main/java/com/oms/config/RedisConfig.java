package com.oms.config;

import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for refresh token, token blacklist, and caching.
 * Only active when {@code spring.data.redis.enabled} is true.
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true",
    matchIfMissing = false)
public class RedisConfig {

    private final String redisHost;
    private final int redisPort;
    private final int redisDatabase;
    private final String redisPassword;
    private final Duration cacheTtl;

    /**
     * Internal ObjectMapper for Redis serialization only — deliberately NOT a
     * Spring bean. Exposing it as a bean would make it the sole
     * {@code ObjectMapper} in the context whenever Redis is enabled
     * (staging/prod), which would stop Spring Boot's {@code JacksonAutoConfiguration}
     * and leak the default-typing mode into every MVC HTTP request/response
     * (breaking any body containing non-final types like {@code BigDecimal}
     * or {@code List}). Kept as an instance field so {@link #redisTemplate}
     * and {@link #cacheConfiguration} share one identical instance (F-18).
     */
    private final ObjectMapper redisObjectMapper;

    // V4-02: constructor injection instead of field-level @Value.
    public RedisConfig(
            @Value("${spring.data.redis.host:localhost}") String redisHost,
            @Value("${spring.data.redis.port:6379}") int redisPort,
            @Value("${spring.data.redis.database:0}") int redisDatabase,
            @Value("${spring.data.redis.password:}") String redisPassword,
            @Value("${spring.data.redis.cache.ttl:1h}") Duration cacheTtl) {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.redisDatabase = redisDatabase;
        this.redisPassword = redisPassword;
        this.cacheTtl = cacheTtl;
        this.redisObjectMapper = createRedisObjectMapper();
    }

    /**
     * Shared {@link ObjectMapper} for Redis serialization.
     * <p>
     * Uses {@link BasicPolymorphicTypeValidator} with an explicit whitelist
     * ({@code com.oms.*}, {@code java.util.*}, {@code java.lang.*},
     * {@code java.math.*}, {@code java.time.*}) so only known DTO/value types
     * can be deserialized, preventing arbitrary-class deserialization
     * (fixes F-11 / DefaultTyping vulnerability).
     * <p>
     * Returns the shared internal instance — this is a plain factory method used
     * by {@link #redisTemplate} and {@link #cacheConfiguration} (and kept public
     * for unit tests); it is not registered as an {@code ObjectMapper} bean so
     * the MVC layer keeps Spring Boot's default ObjectMapper.
     */
    public ObjectMapper redisObjectMapper() {
        return redisObjectMapper;
    }

    private static ObjectMapper createRedisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.oms.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                // Value types used by cached DTOs. BigDecimal/BigInteger are
                // non-final, so default typing writes their type ids
                // (e.g. ["java.math.BigDecimal", 25000.00]); without these the
                // deserializer would reject every @Cacheable read.
                .allowIfSubType("java.math.")
                .allowIfSubType("java.time.")
                .build(),
            // EVERYTHING (not NON_FINAL): cached values are java records
            // (ProductResponse/OrderResponse), which are final classes. With
            // NON_FINAL Jackson writes no type id for them, so the cache read
            // fails with "need Array value to contain As.WRAPPER_ARRAY type
            // information". EVERYTHING emits type ids for final types too.
            ObjectMapper.DefaultTyping.EVERYTHING
        );
        return mapper;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setDatabase(redisDatabase);
        config.setPassword(redisPassword);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer(redisObjectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer(redisObjectMapper);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheTtl)
                .disableCachingNullValues()
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair
                        .fromSerializer(stringSerializer))
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer));
    }

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            RedisCacheConfiguration cacheConfiguration) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration)
                .build();
    }
}