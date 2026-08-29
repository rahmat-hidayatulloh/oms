package com.oms.constant;

/**
 * Error and Problem Detail related constants.
 */
public final class ErrorConstants {

    private ErrorConstants() {
    }

    public static final String PROPERTY_TIMESTAMP = "timestamp";
    public static final String PROPERTY_FIELD_ERRORS = "fieldErrors";
    public static final String PROPERTY_REQUEST_ID = "requestId";
    public static final String MSG_BAD_CREDENTIALS = "Bad credentials";
    public static final String MSG_ACCESS_DENIED = "You don't have permission to access this resource";
    public static final String MSG_VALIDATION_FAILED = "Request validation failed";
    public static final String MSG_MALFORMED_BODY = "Request body is malformed or contains invalid values";
    public static final String MSG_INTERNAL_ERROR = "An unexpected error occurred. Please try again later.";
    public static final String PARAM_PREFIX_INVALID = "Invalid value for parameter: ";
    public static final String PARAM_EXPECTED_TYPE = " (expected type: ";

    public static final String MSG_PRODUCT_NAME_EXISTS = "Product name '%s' already exists";
    public static final String MSG_QUANTITY_POSITIVE = "Quantity for product '%s' must be positive";
    public static final String MSG_INSUFFICIENT_STOCK = "Insufficient stock for product '%s': "
            + "requested %d, available %d";
    public static final String MSG_USER_NOT_FOUND = "User not found with username: %s";
}
