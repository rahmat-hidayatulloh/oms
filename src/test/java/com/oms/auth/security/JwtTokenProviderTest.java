package com.oms.auth.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link JwtTokenProvider}.
 * Tests access token and refresh token generation, validation, and extraction.
 */
class JwtTokenProviderTest {

    private static final String VALID_SECRET = "test-secret-key-that-is-at-least-32-characters-long!!!";
    private static final long EXPIRATION_MS = 3600000L;
    private static final long REFRESH_EXPIRATION_MS = 604800000L;

    private JwtTokenProvider createProvider() {
        return new JwtTokenProvider(VALID_SECRET, EXPIRATION_MS, REFRESH_EXPIRATION_MS);
    }

    @Test
    void constructor_shouldInitializeWithValidSecret() {
        JwtTokenProvider provider = createProvider();
        assertThat(provider).isNotNull();
    }

    @Test
    void constructor_shouldThrowExceptionForShortSecret() {
        assertThatThrownBy(() -> new JwtTokenProvider("short", EXPIRATION_MS, REFRESH_EXPIRATION_MS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void constructor_shouldThrowExceptionForEmptySecret() {
        assertThatThrownBy(() -> new JwtTokenProvider("", EXPIRATION_MS, REFRESH_EXPIRATION_MS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_shouldThrowExceptionForNullSecret() {
        assertThatThrownBy(() -> new JwtTokenProvider(null, EXPIRATION_MS, REFRESH_EXPIRATION_MS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateAccessToken_shouldReturnToken() {
        JwtTokenProvider provider = createProvider();
        String token = provider.generateAccessToken("testuser", "ADMIN");
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void generateRefreshToken_shouldReturnToken() {
        JwtTokenProvider provider = createProvider();
        String token = provider.generateRefreshToken("testuser", "ADMIN");
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void generateAccessTokenAndRefreshToken_shouldBeDifferent() {
        JwtTokenProvider provider = createProvider();
        String accessToken = provider.generateAccessToken("testuser", "ADMIN");
        String refreshToken = provider.generateRefreshToken("testuser", "ADMIN");
        assertThat(accessToken).isNotEqualTo(refreshToken);
    }

    @Test
    void extractUsername_shouldReturnUsername() {
        JwtTokenProvider provider = createProvider();
        String token = provider.generateAccessToken("testuser", "ADMIN");
        String username = provider.extractUsername(token);
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void extractRole_shouldReturnRole() {
        JwtTokenProvider provider = createProvider();
        String token = provider.generateAccessToken("testuser", "ADMIN");
        String role = provider.extractRole(token);
        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    void extractTokenType_shouldReturnAccessTokenType() {
        JwtTokenProvider provider = createProvider();
        String token = provider.generateAccessToken("testuser", "ADMIN");
        String tokenType = provider.extractTokenType(token);
        assertThat(tokenType).isEqualTo("access");
    }

    @Test
    void extractTokenType_shouldReturnRefreshTokenType() {
        JwtTokenProvider provider = createProvider();
        String token = provider.generateRefreshToken("testuser", "ADMIN");
        String tokenType = provider.extractTokenType(token);
        assertThat(tokenType).isEqualTo("refresh");
    }

    @Test
    void validateToken_shouldReturnTrueForValidAccessToken() {
        JwtTokenProvider provider = createProvider();
        String token = provider.generateAccessToken("testuser", "ADMIN");
        boolean isValid = provider.validateToken(token);
        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_shouldReturnTrueForValidRefreshToken() {
        JwtTokenProvider provider = createProvider();
        String token = provider.generateRefreshToken("testuser", "ADMIN");
        boolean isValid = provider.validateToken(token);
        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        JwtTokenProvider provider = createProvider();
        boolean isValid = provider.validateToken("invalid-token");
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalseForNullToken() {
        JwtTokenProvider provider = createProvider();
        boolean isValid = provider.validateToken(null);
        assertThat(isValid).isFalse();
    }

    @Test
    void extractUsername_shouldThrowExceptionForInvalidToken() {
        JwtTokenProvider provider = createProvider();
        assertThatThrownBy(() -> provider.extractUsername("invalid-token"))
                .isInstanceOf(io.jsonwebtoken.MalformedJwtException.class);
    }

    @Test
    void extractRole_shouldThrowExceptionForInvalidToken() {
        JwtTokenProvider provider = createProvider();
        assertThatThrownBy(() -> provider.extractRole("invalid-token"))
                .isInstanceOf(io.jsonwebtoken.MalformedJwtException.class);
    }

    @Test
    void extractTokenType_shouldThrowExceptionForInvalidToken() {
        JwtTokenProvider provider = createProvider();
        assertThatThrownBy(() -> provider.extractTokenType("invalid-token"))
                .isInstanceOf(io.jsonwebtoken.MalformedJwtException.class);
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        JwtTokenProvider provider = new JwtTokenProvider(VALID_SECRET, -1000L, REFRESH_EXPIRATION_MS);
        String token = provider.generateAccessToken("testuser", "ADMIN");
        boolean isValid = provider.validateToken(token);
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalseForTokenWithWrongSignature() {
        JwtTokenProvider provider = createProvider();
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "another-secret-key-that-is-also-at-least-32-characters!!!",
                EXPIRATION_MS, REFRESH_EXPIRATION_MS);
        String token = otherProvider.generateAccessToken("testuser", "ADMIN");
        boolean isValid = provider.validateToken(token);
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalseForEmptyToken() {
        JwtTokenProvider provider = createProvider();
        boolean isValid = provider.validateToken("");
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalseForMalformedToken() {
        JwtTokenProvider provider = createProvider();
        boolean isValid = provider.validateToken("not.a.jwt");
        assertThat(isValid).isFalse();
    }
}
