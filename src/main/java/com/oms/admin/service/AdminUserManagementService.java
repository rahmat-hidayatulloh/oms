package com.oms.admin.service;

import com.oms.audit.annotation.Auditable;
import com.oms.admin.dto.UserResponse;
import com.oms.auth.entity.User;
import com.oms.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for admin user management operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserManagementService
        implements AdminUserManagementServiceInterface {

    private final UserRepository userRepository;

    @Override
    @Auditable(action = "READ", entityType = "User")
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);

        log.debug("Found {} users on page {}",
                users.getNumberOfElements(), users.getNumber());

        return users.map(this::toResponse);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
