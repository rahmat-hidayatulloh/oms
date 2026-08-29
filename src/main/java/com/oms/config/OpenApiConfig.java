package com.oms.config;

import com.oms.constant.OpenApiConstants;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenAPI / Swagger configuration with JWT Bearer token support.
 * <p>
 * Adds an "Authorize" button in Swagger UI so users can paste their JWT token
 * obtained from {@code POST /api/auth/login} and test secured endpoints.
 */
@Configuration
public class OpenApiConfig {

    
    
    

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OMS API")
                        .version("1.0.0")
                        .description("Order Management System API - Documentation"))
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