package com.oms.constant;

/**
 * Security and JWT related constants.
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_TOKEN_TYPE = "token_type";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final int MIN_SECRET_LENGTH = 32;
    public static final String ERROR_SECRET_EMPTY = "JWT secret cannot be null or empty";
    public static final String ERROR_SECRET_LENGTH = "JWT secret must be at least 256 bits (32 bytes) "
            + "for HS256 algorithm";
}
