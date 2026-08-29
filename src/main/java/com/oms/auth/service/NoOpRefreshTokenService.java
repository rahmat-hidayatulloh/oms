package com.oms.auth.service;

import com.oms.constant.ErrorConstants;
import com.oms.exception.BadRequestException;
import com.oms.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * No-op implementation of RefreshTokenService for when Redis is disabled.
 * Provides empty implementations that log at debug level.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class NoOpRefreshTokenService implements RefreshTokenServiceInterface {

    private final UserRepository userRepository;

    @Override
    public void saveRefreshToken(UUID userId, String token) {
        log.debug("Redis disabled - refresh token not saved for user {}", userId);
    }

    @Override
    public String findRefreshToken(UUID userId) {
        return null;
    }

    @Override
    public void deleteRefreshToken(UUID userId) {
        log.debug("Redis disabled - refresh token not deleted for user {}", userId);
    }

    @Override
    public void blacklistAccessToken(String token) {
        log.debug("Redis disabled - access token not blacklisted");
    }

    @Override
    public boolean isAccessTokenBlacklisted(String token) {
        return false;
    }

    @Override
    public UUID resolveUserId(String username) {
        return userRepository.findByUsername(username)
                .map(user -> user.getId())
                .orElseThrow(() -> new BadRequestException(
                        ErrorConstants.MSG_USER_NOT_FOUND + username));
    }
}
