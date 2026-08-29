package com.oms.auth.dto;

import com.oms.auth.validation.PasswordStrength;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for user login.
 */
public record LoginRequest(
                @NotBlank(message = "Username is required") String username,

                @NotBlank(message = "Password is required") @PasswordStrength String password) {
}