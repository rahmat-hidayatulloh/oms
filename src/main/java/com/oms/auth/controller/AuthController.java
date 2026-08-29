package com.oms.auth.controller;

import java.util.UUID;

import com.oms.audit.annotation.Auditable;
import com.oms.config.annotation.ApiVersion;
import com.oms.order.OrderConstants;
import com.oms.constant.SecurityConstants;
import com.oms.auth.dto.LoginRequest;
import com.oms.auth.dto.LoginResponse;
import com.oms.auth.dto.RefreshTokenRequest;
import com.oms.auth.dto.RegisterRequest;
import com.oms.auth.dto.RegisterResponse;
import com.oms.exception.BadRequestException;
import com.oms.ratelimit.RateLimit;
import com.oms.auth.security.JwtTokenProvider;
import com.oms.auth.security.UserDetailsAdapter;
import com.oms.auth.service.RefreshTokenServiceInterface;
import com.oms.auth.service.RegisterServiceInterface;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for user authentication (login, refresh, logout) - v1.
 * Does NOT depend on UserRepository directly \u2014 user lookups are delegated
 * to RefreshTokenServiceInterface (DIP compliance, SOLID D-1 fix).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@ApiVersion("v1")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Auth: login, refresh, logout. Public endpoints for JWT token management.")
@RateLimit(name = "auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenServiceInterface refreshTokenService;
    private final RegisterServiceInterface registerService;

    @Operation(
        summary = "Register a new user account",
        description = "Creates a new CUSTOMER account with username, email, and password. "
                + "Password must be at least 8 characters with uppercase, lowercase, number, and special character.",
        responses = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or username/email already taken"),
            @ApiResponse(responseCode = "429", description = "Too many requests \u2014 rate limit exceeded")
        })
    @PostMapping("/register")
    @RateLimit(name = "auth-register", capacity = 5, refillTokens = 5, refillPeriodSeconds = 60)
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = registerService.register(request);
        log.info("New user registered: '{}'", request.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Authenticate user and return JWT tokens",
        description = "Authenticates a user with username/password. Returns access token (1h) and refresh token (7d).",
        responses = {
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "429", description = "Too many requests \u2014 rate limit exceeded")
        })
    @PostMapping("/login")
    @Auditable(action = "LOGIN", entityType = "User")
    @RateLimit(name = "auth-login", capacity = 5, refillTokens = 5, refillPeriodSeconds = 60)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserDetailsAdapter userDetails = (UserDetailsAdapter) authentication.getPrincipal();
        String username = userDetails.getUsername();
        String role = userDetails.getUser().getRole().name();

        String accessToken = jwtTokenProvider.generateAccessToken(username, role);
        String refreshToken = jwtTokenProvider.generateRefreshToken(username, role);

        refreshTokenService.saveRefreshToken(userDetails.getUser().getId(), refreshToken);

        log.info("User '{}' authenticated successfully", username);
        return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken));
    }

    @Operation(
        summary = "Refresh access token using refresh token",
        description = "Validates the refresh token, detects replay, and rotates both tokens. "
                + "On replay detection, all tokens for the user are revoked.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tokens refreshed",
                content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid, expired, or replayed refresh token")
        })
    @PostMapping("/refresh")
    @RateLimit(name = "auth")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String incomingToken = request.refreshToken();

        try {
            if (!jwtTokenProvider.validateToken(incomingToken)) {
                throw new BadRequestException(OrderConstants.MSG_INVALID_REFRESH_TOKEN);
            }

            String tokenType = jwtTokenProvider.extractTokenType(incomingToken);
            if (!SecurityConstants.TOKEN_TYPE_REFRESH.equals(tokenType)) {
                throw new BadRequestException(OrderConstants.MSG_INVALID_REFRESH_TOKEN);
            }

            String username = jwtTokenProvider.extractUsername(incomingToken);
            String role = jwtTokenProvider.extractRole(incomingToken);

            if (username == null || role == null) {
                throw new BadRequestException(OrderConstants.MSG_INVALID_REFRESH_TOKEN);
            }

            UUID userId = refreshTokenService.resolveUserId(username);
            String storedToken = refreshTokenService.findRefreshToken(userId);

            if (storedToken == null || !storedToken.equals(incomingToken)) {
                log.warn("Refresh token replay or mismatch for user '{}'", username);
                if (storedToken != null) {
                    refreshTokenService.deleteRefreshToken(userId);
                }
                throw new BadRequestException(OrderConstants.MSG_INVALID_REFRESH_TOKEN);
            }

            String newAccessToken = jwtTokenProvider.generateAccessToken(username, role);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(username, role);

            refreshTokenService.saveRefreshToken(userId, newRefreshToken);

            log.info("Refreshed tokens for user '{}' (rotation)", username);
            return ResponseEntity.ok(new LoginResponse(newAccessToken, newRefreshToken));
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception ex) {
            log.warn("Refresh token validation failed: {}", ex.getMessage());
            throw new BadRequestException(OrderConstants.MSG_INVALID_REFRESH_TOKEN);
        }
    }

    @Operation(
        summary = "Logout and revoke refresh token",
        description = "Validates the refresh token and removes it from storage so it cannot be used again.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token")
        })
    @PostMapping("/logout")
    @RateLimit(name = "auth")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        String incomingToken = request.refreshToken();

        if (!jwtTokenProvider.validateToken(incomingToken)) {
            throw new BadRequestException(OrderConstants.MSG_INVALID_REFRESH_TOKEN);
        }

        String tokenType = jwtTokenProvider.extractTokenType(incomingToken);
        if (!SecurityConstants.TOKEN_TYPE_REFRESH.equals(tokenType)) {
            throw new BadRequestException(OrderConstants.MSG_INVALID_REFRESH_TOKEN);
        }

        String username = jwtTokenProvider.extractUsername(incomingToken);
        if (username == null) {
            throw new BadRequestException(OrderConstants.MSG_INVALID_REFRESH_TOKEN);
        }

        UUID userId = refreshTokenService.resolveUserId(username);
        refreshTokenService.deleteRefreshToken(userId);

        log.info("User '{}' logged out \u2014 refresh token revoked", username);
        return ResponseEntity.ok().build();
    }
}
