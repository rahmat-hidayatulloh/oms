package com.oms.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link PaymentApiProperties} ({@code app.payment.api.base-url},
 * {@code app.payment.api.health-path}) so the health indicator can
 * constructor-inject the environment-aware external API configuration
 * (same registration pattern as {@link ErrorPropertiesConfig}).
 */
@Configuration
@EnableConfigurationProperties(PaymentApiProperties.class)
public class PaymentApiPropertiesConfig {
}