package com.oms.auth.service;

import com.oms.audit.annotation.Auditable;
import com.oms.auth.repository.UserRepository;
import com.oms.constant.ErrorConstants;
import com.oms.order.OrderConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Service for managing refresh tokens and access token blacklist using Redis.
 * Only active when {@code spring.data.redis.enabled} is true.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RefreshTokenService implements RefreshTokenServiceInterface {

    private final int refreshTokenExpirationDays;
    private final Duration blacklistDuration;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    // V4-02: constructor injection instead of field-level @Value.
    public RefreshTokenService(
            @Value("${app.refresh-token.expiration-days:7}") int refreshTokenExpirationDays,
            @Value("${app.redis.blacklist-duration:1h}") Duration blacklistDuration,
            RedisTemplate<String, Object> redisTemplate,
            UserRepository userRepository) {
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
        this.blacklistDuration = blacklistDuration;
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
    }

    @Override
    @Auditable(action = "CREATE", entityType = "RefreshToken")
    public void saveRefreshToken(UUID userId, String token) {
        String key = OrderConstants.REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, token, Duration.ofDays(refreshTokenExpirationDays));
        log.debug("Saved refresh token for user {}", userId);
    }

    @Override
    public String findRefreshToken(UUID userId) {
        String key = OrderConstants.REFRESH_TOKEN_PREFIX + userId;
        Object token = redisTemplate.opsForValue().get(key);
        return token != null ? token.toString() : null;
    }

    @Override
    @Auditable(action = "DELETE", entityType = "RefreshToken")
    public void deleteRefreshToken(UUID userId) {
        String key = OrderConstants.REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("Deleted refresh token for user {}", userId);
    }

    @Override
    @Auditable(action = "BLACKLIST", entityType = "AccessToken")
    public void blacklistAccessToken(String token) {
        String key = OrderConstants.BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "blacklisted", blacklistDuration);
        log.debug("Blacklisted access token");
    }

    public boolean isAccessTokenBlacklisted(String token) {
        String key = OrderConstants.BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public UUID resolveUserId(String username) {
        return userRepository.findByUsername(username)
                .map(user -> user.getId())
                .orElseThrow(() -> new com.oms.exception.BadRequestException(
                        ErrorConstants.MSG_USER_NOT_FOUND + username));
    }
}