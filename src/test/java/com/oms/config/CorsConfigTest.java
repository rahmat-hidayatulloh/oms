package com.oms.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CORS configuration to verify:
 * <ul>
 *   <li>CorsProperties are correctly applied</li>
 *   <li>CorsConfigurationSource is created properly</li>
 *   <li>Origins come from properties, not hardcoded (F-12 fix)</li>
 * </ul>
 */
class CorsConfigTest {

    @Test
    void shouldCreateCorsPropertiesWithDevDefaults() {
        CorsProperties props = new CorsProperties();

        assertThat(props.getAllowedOrigins())
            .containsExactly("http://localhost:3000");
        assertThat(props.getAllowedMethods())
            .contains("GET", "POST", "PUT",
                "PATCH", "DELETE", "OPTIONS");
        assertThat(props.getAllowedHeaders())
            .containsExactly("*");
        assertThat(props.isAllowCredentials()).isTrue();
    }

    @Test
    void shouldApplyCustomOriginsFromProperties() {
        CorsProperties props = new CorsProperties();
        props.setAllowedOrigins(List.of(
            "https://oms.com", "https://admin.oms.com"));

        assertThat(props.getAllowedOrigins())
            .containsExactly(
                "https://oms.com", "https://admin.oms.com");
    }

    @Test
    void shouldDisableCredentialsWhenConfigured() {
        CorsProperties props = new CorsProperties();
        props.setAllowCredentials(false);

        assertThat(props.isAllowCredentials()).isFalse();
    }

    @Test
    void shouldCreateCorsConfigurationSource() {
        CorsProperties props = new CorsProperties();
        props.setAllowedOrigins(List.of("https://oms.com"));

        SecurityCorsConfig corsConfig = new SecurityCorsConfig(props);
        CorsConfigurationSource source =
            corsConfig.corsConfigurationSource();

        assertThat(source).isInstanceOf(
            UrlBasedCorsConfigurationSource.class);

        MockHttpServletRequest request =
            new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("Origin", "https://oms.com");
        CorsConfiguration config =
            source.getCorsConfiguration(request);
        assertThat(config).isNotNull();
        assertThat(config.getAllowedOriginPatterns())
            .containsExactly("https://oms.com");
        assertThat(config.getAllowCredentials())
            .isTrue();
    }

    @Test
    void shouldReturnConfigForAllPaths() {
        CorsProperties props = new CorsProperties();
        SecurityCorsConfig corsConfig = new SecurityCorsConfig(props);
        CorsConfigurationSource source =
            corsConfig.corsConfigurationSource();

        MockHttpServletRequest request =
            new MockHttpServletRequest("GET", "/other/path");
        CorsConfiguration config =
            source.getCorsConfiguration(request);
        assertThat(config).isNotNull();
    }
}
