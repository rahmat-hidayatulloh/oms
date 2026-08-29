package com.oms.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for refresh token endpoint.
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}