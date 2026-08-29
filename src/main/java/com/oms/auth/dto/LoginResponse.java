package com.oms.auth.dto;

/**
 * Response payload returned after successful authentication.
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
    public LoginResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer");
    }
}