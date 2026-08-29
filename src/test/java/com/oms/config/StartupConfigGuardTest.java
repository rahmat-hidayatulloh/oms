package com.oms.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for {@link StartupConfigGuard}.
 */
@SpringBootTest
@ActiveProfiles("test")
class StartupConfigGuardTest {

    @Autowired
    private Environment env;

    @Test
    void shouldPassForTestProfile() {
        assertThatNoException().isThrownBy(() -> new StartupConfigGuard(env).validate());
    }

    @Test
    void shouldRejectMissingJwtSecretInProdLikeProfile() {
        Environment stubbed = mock(Environment.class);
        when(stubbed.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(stubbed.getProperty("jwt.secret")).thenReturn(null);
        when(stubbed.getProperty("spring.datasource.password", "")).thenReturn("P@ssw0rd");

        StartupConfigGuard guard = new StartupConfigGuard(stubbed);
        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret is not configured");
    }

    @Test
    void shouldRejectShortJwtSecretInProdLikeProfile() {
        Environment stubbed = mock(Environment.class);
        when(stubbed.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(stubbed.getProperty("jwt.secret")).thenReturn("short");
        when(stubbed.getProperty("spring.datasource.password", "")).thenReturn("secret");

        StartupConfigGuard guard = new StartupConfigGuard(stubbed);
        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret is too short");
    }

    @Test
    void shouldRejectWeakDbPasswordInProdLikeProfile() {
        Environment stubbed = mock(Environment.class);
        when(stubbed.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(stubbed.getProperty("jwt.secret")).thenReturn("valid-jwt-secret-key-12345678901234567890");
        when(stubbed.getProperty("spring.datasource.password", "")).thenReturn("P@ssw0rd");

        StartupConfigGuard guard = new StartupConfigGuard(stubbed);
        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Database password is a known weak/default value");
    }

    @Test
    void shouldNotBypassValidationWhenNoActiveProfile() {
        // V4-03: an empty profile set (e.g. SPRING_PROFILES_ACTIVE unset in a
        // prod deployment) must NOT be treated as dev. The guard must still
        // enforce JWT secret validation instead of silently bypassing it.
        Environment stubbed = mock(Environment.class);
        when(stubbed.getActiveProfiles()).thenReturn(new String[]{});
        when(stubbed.getProperty("jwt.secret")).thenReturn(null);
        when(stubbed.getProperty("spring.datasource.password", "")).thenReturn("");

        StartupConfigGuard guard = new StartupConfigGuard(stubbed);
        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret is not configured");
    }

    @Test
    void shouldStillTreatDevProfileAsExempt() {
        // V4-03: the dev profile remains exempt from JWT secret enforcement so
        // local development is not blocked.
        Environment stubbed = mock(Environment.class);
        when(stubbed.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(stubbed.getProperty("jwt.secret")).thenReturn(null);
        when(stubbed.getProperty("spring.datasource.password", "")).thenReturn("");

        StartupConfigGuard guard = new StartupConfigGuard(stubbed);
        assertThatNoException().isThrownBy(guard::validate);
    }
}
