package com.oms.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link ErrorProperties} ({@code app.error.base-url}) so the
 * exception-handler family can constructor-inject the externalized
 * RFC 7807 error type base URL (same registration pattern as
 * {@link SecurityCorsConfig} for {@link CorsProperties}).
 */
@Configuration
@EnableConfigurationProperties(ErrorProperties.class)
public class ErrorPropertiesConfig {
}