package com.oms.auth.service;

import java.util.UUID;

/**
 * Interface for refresh token and access token blacklist operations.
 */
public interface RefreshTokenServiceInterface {

    void saveRefreshToken(UUID userId, String token);

    String findRefreshToken(UUID userId);

    void deleteRefreshToken(UUID userId);

    void blacklistAccessToken(String token);

    boolean isAccessTokenBlacklisted(String token);

    /**
     * Resolves a user ID from username via the user repository.
     * Removes the need for controllers to depend on UserRepository directly (DIP).
     *
     * @param username the username to resolve
     * @return the user UUID
     * @throws com.oms.exception.BadRequestException if user not found
     */
    UUID resolveUserId(String username);
}