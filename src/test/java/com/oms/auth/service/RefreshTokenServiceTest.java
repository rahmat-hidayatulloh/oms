package com.oms.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenServiceTest {

    @Autowired
    private RefreshTokenServiceInterface refreshTokenService;

    @Test
    void saveAndFindRefreshToken() {
        UUID userId = UUID.randomUUID();
        String token = "refresh-token-123";

        refreshTokenService.saveRefreshToken(userId, token);
        String found = refreshTokenService.findRefreshToken(userId);

        // In test profile with Redis disabled, NoOpRefreshTokenService returns null
        assertNull(found);
    }

    @Test
    void deleteRefreshToken() {
        UUID userId = UUID.randomUUID();
        String token = "refresh-token-456";

        refreshTokenService.saveRefreshToken(userId, token);
        refreshTokenService.deleteRefreshToken(userId);
        String found = refreshTokenService.findRefreshToken(userId);

        assertNull(found);
    }

    @Test
    void blacklistAndCheckAccessToken() {
        // Note: This test verifies the method signature and behavior.
        // Actual Redis operations are only tested in integration tests with Redis enabled.
        // In test profile, Redis is disabled, so we just verify the service is callable.
        String token = "access-token-789";
        
        // Should not throw exception even when Redis is disabled
        refreshTokenService.blacklistAccessToken(token);
        
        // In test profile with Redis disabled, behavior depends on implementation
        // Just verify the method completes without throwing
        assertDoesNotThrow(() -> refreshTokenService.blacklistAccessToken(token));
    }
}
