package com.oms.auth.service;

import com.oms.audit.annotation.Auditable;
import com.oms.auth.dto.RegisterRequest;
import com.oms.auth.dto.RegisterResponse;
import com.oms.auth.entity.Role;
import com.oms.auth.entity.User;
import com.oms.exception.BadRequestException;
import com.oms.auth.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for user registration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterService implements RegisterServiceInterface {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user account.
     *
     * @param request the registration request containing username, email, and
     *                password
     * @return the registration response
     * @throws BadRequestException if username or email already exists
     */
    @Override
    @Auditable(action = "CREATE", entityType = "User")
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username '" + request.username() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email '" + request.email() + "' is already registered");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER) // Default role for new registrations
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .isEnabled(true)
                .build();

        userRepository.save(user);

        log.info("User '{}' registered successfully with role {}", user.getUsername(), user.getRole());
        return new RegisterResponse(user.getUsername(), user.getEmail(), user.getRole());
    }
}