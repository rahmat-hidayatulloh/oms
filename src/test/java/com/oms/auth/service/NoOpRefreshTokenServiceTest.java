package com.oms.auth.service;

import com.oms.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NoOpRefreshTokenServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final NoOpRefreshTokenService service = new NoOpRefreshTokenService(userRepository);

    @Test
    void saveRefreshToken_shouldNotThrow() {
        service.saveRefreshToken(UUID.randomUUID(), "token");
    }

    @Test
    void findRefreshToken_shouldReturnNull() {
        String result = service.findRefreshToken(UUID.randomUUID());
        assertThat(result).isNull();
    }

    @Test
    void deleteRefreshToken_shouldNotThrow() {
        service.deleteRefreshToken(UUID.randomUUID());
    }

    @Test
    void blacklistAccessToken_shouldNotThrow() {
        service.blacklistAccessToken("some-token");
    }

    @Test
    void isAccessTokenBlacklisted_shouldReturnFalse() {
        boolean result = service.isAccessTokenBlacklisted("some-token");
        assertThat(result).isFalse();
    }
}
