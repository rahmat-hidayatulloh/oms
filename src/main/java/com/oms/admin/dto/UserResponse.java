package com.oms.admin.dto;

import com.oms.auth.entity.Role;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user data in admin endpoints.
 *
 * @param id              the user UUID
 * @param username        the username
 * @param email           the email address
 * @param role            the user role
 * @param isEnabled       whether the account is enabled
 * @param createdAt       the creation timestamp
 */
public record UserResponse(
        UUID id,
        String username,
        String email,
        Role role,
        boolean isEnabled,
        LocalDateTime createdAt
) {
}