package com.oms.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.List;

/**
 * Startup configuration guard.
 * <p>
 * Validates critical security-related configuration on application startup:
 * <ul>
 *   <li>{@code JWT_SECRET} must be set and strong enough in non-dev profiles</li>
 *   <li>Database password must not be a known weak/default value</li>
 * </ul>
 */
@Component
public class StartupConfigGuard {

    private static final String[] WEAK_DB_PASSWORDS = {
        "P@ssw0rd",
        "password",
        "admin",
        "123456",
        "postgres",
        "sa"
    };

    private final Environment env;

    public StartupConfigGuard(Environment env) {
        this.env = env;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        String[] activeProfiles = env.getActiveProfiles();
        // V4-03: do NOT treat an empty profile set as dev. If no profile is
        // active (e.g. SPRING_PROFILES_ACTIVE unset in a prod deployment), the
        // guard must still enforce JWT secret validation rather than bypass it.
        boolean isDev = List.of(activeProfiles).contains("dev");

        if (!isDev) {
            String jwtSecret = env.getProperty("jwt.secret");
            if (jwtSecret == null || jwtSecret.isBlank()) {
                throw new IllegalStateException(
                        "JWT secret is not configured. Set the JWT_SECRET environment variable.");
            }
            if (jwtSecret.length() < 32) {
                throw new IllegalStateException(
                        "JWT secret is too short. It must be at least 32 characters for HS256.");
            }
        }

        String dbPassword = env.getProperty("spring.datasource.password", "");
        if (!dbPassword.isBlank()) {
            String lower = dbPassword.toLowerCase(Locale.ROOT);
            for (String weak : WEAK_DB_PASSWORDS) {
                if (lower.equals(weak.toLowerCase(Locale.ROOT))) {
                    throw new IllegalStateException(
                            "Database password is a known weak/default value. Use a strong password.");
                }
            }
        }
    }
}