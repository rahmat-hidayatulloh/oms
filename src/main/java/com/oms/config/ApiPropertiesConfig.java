package com.oms.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link ApiProperties} ({@code app.api.base-url}) so OpenAPI
 * documentation can reference the environment-aware public API base URL
 * (same registration pattern as {@link ErrorPropertiesConfig}).
 */
@Configuration
@EnableConfigurationProperties(ApiProperties.class)
public class ApiPropertiesConfig {
}