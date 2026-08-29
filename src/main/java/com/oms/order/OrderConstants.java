package com.oms.order;

/**
 * Order and entity-related constants.
 */
public final class OrderConstants {

    private OrderConstants() {
    }

    public static final String ENTITY_PRODUCT = "Product";
    public static final String ENTITY_ORDER = "Order";
    public static final String ENTITY_USER = "User";

    public static final String ORDER_NUMBER_PREFIX = "OMS-";
    public static final String ORDER_NUMBER_DATE_FORMAT = "yyyyMMdd";
    public static final int ORDER_NUMBER_RANDOM_LENGTH = 8;

    public static final String DEFAULT_FALLBACK_EMAIL = "unknown@example.com";
    public static final String MSG_INVALID_REFRESH_TOKEN = "Invalid refresh token";
    public static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    public static final String BLACKLIST_PREFIX = "blacklist:";

    public static final String EMAIL_EXECUTOR_NAME = "EmailExecutor-";

    public static final int DEFAULT_PAGE_NUMBER = 0;

    public static final String SERVICE_NAME_EXTERNAL_API = "External Payment API";
    public static final String SERVICE_NAME_REDIS = "Redis";

    public static final String APP_TIMEZONE = "Asia/Jakarta";
}
