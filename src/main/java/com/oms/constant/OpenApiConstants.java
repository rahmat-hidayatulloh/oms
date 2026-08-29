package com.oms.constant;

/**
 * OpenAPI / Swagger and API versioning constants.
 */
public final class OpenApiConstants {

    private OpenApiConstants() {
    }

    public static final String SCHEME_NAME_BEARER_AUTH = "bearerAuth";
    public static final String SCHEME_BEARER = "bearer";
    public static final String FORMAT_JWT = "JWT";

    public static final String DEFAULT_API_VERSION = "v1";
    public static final String HEADER_DEPRECATED = "X-API-Deprecated";
    public static final String HEADER_REPLACEMENT = "X-API-Deprecation-Replacement";
    public static final String HEADER_SUNSET = "Sunset";
    public static final String SUNSET_DATE = "2027-01-01";

    public static final String VERSION_CURRENT = "currentVersion";
    public static final String VERSION_AVAILABLE = "availableVersions";
    public static final String VERSION_DEPRECATED = "deprecatedVersions";
    public static final String VERSION_LATEST = "latestVersion";
    public static final String VERSION_API_DOCS = "apiDocumentation";
    public static final String VERSION_SERVER_TIME = "serverTime";
    public static final String SWAGGER_UI_PATH = "/swagger-ui/index.html";
}
