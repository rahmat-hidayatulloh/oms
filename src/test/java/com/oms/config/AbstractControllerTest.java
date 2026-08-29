package com.oms.config;

import com.oms.auth.entity.Role;
import com.oms.auth.entity.User;
import com.oms.auth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Base class for controller tests providing common authentication setup.
 * <p>
 * This is a unit test base class that mocks SecurityContext without database
 * access.
 * Reduces code duplication for SecurityContext mocking across controller tests.
 */
public abstract class AbstractControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Sets up authentication for a customer user.
     *
     * @param username the username
     * @return the authenticated user ID
     */
    protected UUID setupCustomerAuthentication(String username) {
        setupAuthentication(username, "ROLE_CUSTOMER");
        return UUID.randomUUID();
    }

    /**
     * Sets up authentication for an admin user.
     *
     * @param username the username
     * @return the authenticated user ID
     */
    protected UUID setupAdminAuthentication(String username) {
        setupAuthentication(username, "ROLE_ADMIN");
        return UUID.randomUUID();
    }

    /**
     * Sets up authentication with custom role.
     *
     * @param username the username
     * @param role     the role (e.g., "ROLE_CUSTOMER", "ROLE_ADMIN")
     */
    protected void setupAuthentication(String username, String role) {
        org.springframework.security.core.Authentication authentication = mock(
                org.springframework.security.core.Authentication.class);
        doReturn(username).when(authentication).getName();
        doReturn(List.of(new SimpleGrantedAuthority(role)))
                .when(authentication).getAuthorities();

        SecurityContext securityContext = mock(SecurityContext.class);
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
    }
}