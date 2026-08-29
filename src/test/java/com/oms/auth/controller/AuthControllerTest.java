package com.oms.auth.controller;

import com.oms.auth.dto.LoginRequest;
import com.oms.auth.dto.LoginResponse;
import com.oms.auth.dto.RefreshTokenRequest;
import com.oms.auth.entity.Role;
import com.oms.auth.entity.User;
import com.oms.exception.BadRequestException;
import com.oms.auth.security.JwtTokenProvider;
import com.oms.auth.security.UserDetailsAdapter;
import com.oms.auth.service.RefreshTokenServiceInterface;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenServiceInterface refreshTokenService;

    @InjectMocks
    private AuthController authController;

    @Test
    void login_shouldReturnTokens() {
        LoginRequest request = new LoginRequest("testuser", "password");
        UUID userId = UUID.randomUUID();
        User mockUser = User.builder()
                .id(userId)
                .username("testuser")
                .password("password")
                .role(Role.CUSTOMER)
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new UserDetailsAdapter(mockUser), "password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateAccessToken("testuser", "CUSTOMER"))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken("testuser", "CUSTOMER"))
                .thenReturn("refresh-token");

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("access-token");
        assertThat(response.getBody().refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenService).saveRefreshToken(userId, "refresh-token");
    }

    @Test
    void refresh_shouldReturnNewTokens_whenValidRefreshToken() {
        UUID userId = UUID.randomUUID();
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.extractTokenType("valid-refresh-token")).thenReturn("refresh");
        when(jwtTokenProvider.extractUsername("valid-refresh-token")).thenReturn("testuser");
        when(jwtTokenProvider.extractRole("valid-refresh-token")).thenReturn("CUSTOMER");
        when(refreshTokenService.resolveUserId("testuser")).thenReturn(userId);
        when(refreshTokenService.findRefreshToken(userId)).thenReturn("valid-refresh-token");
        when(jwtTokenProvider.generateAccessToken("testuser", "CUSTOMER")).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken("testuser", "CUSTOMER")).thenReturn("new-refresh-token");

        ResponseEntity<LoginResponse> response = authController.refresh(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("new-access-token");
        assertThat(response.getBody().refreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenService).saveRefreshToken(userId, "new-refresh-token");
    }

    @Test
    void refresh_shouldThrowBadRequest_whenTokenInvalid() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");

        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

        assertThatThrownBy(() -> authController.refresh(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void refresh_shouldThrowBadRequest_whenTokenTypeIsAccess() {
        RefreshTokenRequest request = new RefreshTokenRequest("access-token");

        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.extractTokenType("access-token")).thenReturn("access");

        assertThatThrownBy(() -> authController.refresh(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refresh_shouldThrowBadRequest_whenUsernameIsNull() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.extractTokenType("valid-refresh-token")).thenReturn("refresh");
        when(jwtTokenProvider.extractUsername("valid-refresh-token")).thenReturn(null);

        assertThatThrownBy(() -> authController.refresh(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void logout_shouldRevokeRefreshToken() {
        UUID userId = UUID.randomUUID();
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.extractTokenType("valid-refresh-token")).thenReturn("refresh");
        when(jwtTokenProvider.extractUsername("valid-refresh-token")).thenReturn("testuser");
        when(refreshTokenService.resolveUserId("testuser")).thenReturn(userId);

        ResponseEntity<Void> response = authController.logout(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(refreshTokenService).deleteRefreshToken(userId);
    }

    @Test
    void logout_shouldThrowWhenTokenInvalid() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");

        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

        assertThatThrownBy(() -> authController.logout(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void logout_shouldThrowWhenTokenTypeIsAccess() {
        RefreshTokenRequest request = new RefreshTokenRequest("access-token");

        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.extractTokenType("access-token")).thenReturn("access");

        assertThatThrownBy(() -> authController.logout(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void logout_shouldThrowWhenUserNotFound() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.extractTokenType("valid-refresh-token")).thenReturn("refresh");
        when(jwtTokenProvider.extractUsername("valid-refresh-token")).thenReturn("ghost");
        when(refreshTokenService.resolveUserId("ghost")).thenThrow(
                new BadRequestException("User not found: ghost"));

        assertThatThrownBy(() -> authController.logout(request))
                .isInstanceOf(BadRequestException.class);
    }
}
