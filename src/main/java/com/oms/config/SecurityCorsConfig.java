package com.oms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration sourced from {@link CorsProperties} instead of
 * hardcoded values. Origins are environment-specific via YAML profiles.
 * Separated from SecurityFilterChainConfig for single responsibility.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityCorsConfig {

    private final CorsProperties corsProperties;

    public SecurityCorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(
            corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(
            corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(
            corsProperties.getAllowedHeaders());
        configuration.setExposedHeaders(
            corsProperties.getExposedHeaders());
        configuration.setAllowCredentials(
            corsProperties.isAllowCredentials());

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
