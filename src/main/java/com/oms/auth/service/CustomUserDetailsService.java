package com.oms.auth.service;

import com.oms.auth.entity.User;
import com.oms.auth.repository.UserRepository;
import com.oms.constant.ErrorConstants;
import com.oms.auth.security.UserDetailsAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads user details from the database during authentication.
 * The @SQLRestriction on User entity ensures soft-deleted users are excluded.
 * Returns {@link UserDetailsAdapter} instead of raw User entity (SOLID L-2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        ErrorConstants.MSG_USER_NOT_FOUND + username));
        log.debug("Loaded user '{}' with role {}", username, user.getRole());
        return new UserDetailsAdapter(user);
    }
}