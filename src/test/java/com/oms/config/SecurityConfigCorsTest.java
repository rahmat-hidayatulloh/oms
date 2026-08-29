package com.oms.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration-style test for {@link SecurityCorsConfig} CORS setup
 * to verify CORS origins are driven by {@link CorsProperties}
 * and not hardcoded (F-12 fix, SOLID O-2).
 */
class SecurityConfigCorsTest {

    @Test
    void shouldUseDevOriginsByDefault() {
        CorsProperties devProps = new CorsProperties();

        SecurityCorsConfig config =
            new SecurityCorsConfig(devProps);
        CorsConfigurationSource source =
            config.corsConfigurationSource();

        MockHttpServletRequest request =
            new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("Origin", "http://localhost:3000");
        CorsConfiguration cors =
            source.getCorsConfiguration(request);
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOriginPatterns())
            .contains("http://localhost:3000");
    }

    @Test
    void shouldUseProductionOrigins() {
        CorsProperties prodProps = new CorsProperties();
        prodProps.setAllowedOrigins(
            List.of("https://oms.com"));

        SecurityCorsConfig config =
            new SecurityCorsConfig(prodProps);
        CorsConfigurationSource source =
            config.corsConfigurationSource();

        MockHttpServletRequest request =
            new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("Origin", "https://oms.com");
        CorsConfiguration cors =
            source.getCorsConfiguration(request);
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOriginPatterns())
            .containsExactly("https://oms.com");
    }

    @Test
    void shouldUseStagingOrigins() {
        CorsProperties stagingProps = new CorsProperties();
        stagingProps.setAllowedOrigins(
            List.of("https://staging.oms.com"));

        SecurityCorsConfig config =
            new SecurityCorsConfig(stagingProps);
        CorsConfigurationSource source =
            config.corsConfigurationSource();

        MockHttpServletRequest request =
            new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(
            "Origin", "https://staging.oms.com");
        CorsConfiguration cors =
            source.getCorsConfiguration(request);
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOriginPatterns())
            .containsExactly("https://staging.oms.com");
    }

    @Test
    void shouldNotContainHardcodedLocalhost() {
        CorsProperties prodProps = new CorsProperties();
        prodProps.setAllowedOrigins(
            List.of("https://oms.com"));

        SecurityCorsConfig config =
            new SecurityCorsConfig(prodProps);
        CorsConfigurationSource source =
            config.corsConfigurationSource();

        MockHttpServletRequest request =
            new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("Origin", "https://oms.com");
        CorsConfiguration cors =
            source.getCorsConfiguration(request);
        assertThat(cors.getAllowedOriginPatterns())
            .doesNotContain("http://localhost:3000");
    }
}
