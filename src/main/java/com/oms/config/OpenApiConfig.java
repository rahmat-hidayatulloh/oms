package com.oms.config;

import com.oms.constant.OpenApiConstants;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/**
 * OpenAPI / Swagger configuration with JWT Bearer token support.
 * <p>
 * Adds an "Authorize" button in Swagger UI so users can paste their JWT token
 * obtained from {@code POST /api/auth/login} and test secured endpoints.
 * <p>
 * Every value shown in the generated spec comes from configuration
 * ({@code info.application.*}, {@link ApiProperties}) — no environment-specific
 * base URL is hardcoded, and no logic branches on an environment name
 * (standard 4.1 — externalize configuration; 4.5 — no environment detection).
 * The spec deliberately declares no {@code servers[].url} so Swagger UI keeps
 * targeting the host that served the request.
 */
@Configuration
public class OpenApiConfig {

    private final ApiProperties apiProperties;
    private final String appName;
    private final String appVersion;
    private final String apiVersion;

    public OpenApiConfig(
            ApiProperties apiProperties,
            @Value("${info.application.name:oms}") String appName,
            @Value("${info.application.version:1.0.0}") String appVersion,
            @Value("${springdoc.api-version:v1}") String apiVersion) {
        this.apiProperties = apiProperties;
        this.appName = appName;
        this.appVersion = appVersion;
        this.apiVersion = apiVersion;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(appName.toUpperCase(Locale.ROOT) + " API")
                        .version(appVersion)
                        .description("Order Management System API - Documentation"
                                + " (current version: " + apiVersion + ")")
                        .contact(new Contact()
                                .name("OMS API Team")
                                .url(apiProperties.getBaseUrl())))
                .addSecurityItem(new SecurityRequirement().addList(OpenApiConstants.SCHEME_NAME_BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(OpenApiConstants.SCHEME_NAME_BEARER_AUTH, new SecurityScheme()
                                .name(OpenApiConstants.SCHEME_NAME_BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme(OpenApiConstants.SCHEME_BEARER)
                                .bearerFormat(OpenApiConstants.FORMAT_JWT)
                                .description("Paste your JWT token here. Get it from POST /api/auth/login")));
    }
}