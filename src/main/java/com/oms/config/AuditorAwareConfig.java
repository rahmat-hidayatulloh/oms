package com.oms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Provides the current auditor (username) for JPA auditing.
 * <p>
 * Used by {@link org.springframework.data.annotation.CreatedBy} and
 * {@link org.springframework.data.annotation.LastModifiedBy} to automatically
 * populate the createdBy and updatedBy fields.
 * <p>
 * If no authenticated user is found (e.g., during system operations or tests),
 * returns "system" as the default auditor.
 */
@Configuration
public class AuditorAwareConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken) {
                return Optional.of("system");
            }

            return Optional.ofNullable(authentication.getName());
        };
    }
}