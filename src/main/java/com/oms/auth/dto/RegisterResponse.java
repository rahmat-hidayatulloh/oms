package com.oms.auth.dto;

import com.oms.auth.entity.Role;

/**
 * Response payload returned after successful registration.
 */
public record RegisterResponse(
    String username,
    String email,
    Role role,
    String message
) {
    public RegisterResponse(String username, String email, Role role) {
        this(username, email, role, "User registered successfully");
    }
}