package com.oms.config.versioning;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.oms.constant.OpenApiConstants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the API version for incoming requests based on URL path prefix.
 * <p>
 * Extracts the version from paths like {@code /api/v1/products} or {@code /api/v2/orders}.
 * If no version is specified, defaults to {@code v1}.
 * <p>
 * This resolver also maintains a registry of deprecated versions for
 * generating deprecation warnings in response headers.
 */
@Slf4j
@Component
public class VersioningResolver {

    

    private final Map<String, String> deprecatedVersions = new ConcurrentHashMap<>();

    public VersioningResolver() {
    }

    /**
     * Extracts the API version from the request URI.
     *
     * @param request the incoming HTTP request
     * @return the API version (e.g., "v1", "v2"), or "v1" if not specified
     */
    public String resolveVersion(HttpServletRequest request) {
        String uri = request.getRequestURI();

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("/api/v(\\d+)/");
        java.util.regex.Matcher matcher = pattern.matcher(uri);

        if (matcher.find()) {
            return "v" + matcher.group(1);
        }

        return OpenApiConstants.DEFAULT_API_VERSION;
    }

    /**
     * Checks if the given version is deprecated.
     *
     * @param version the API version to check
     * @return the replacement version if deprecated, or {@code null} if not deprecated
     */
    public String getDeprecationReplacement(String version) {
        return deprecatedVersions.get(version);
    }

    /**
     * Registers a version as deprecated with a replacement version.
     *
     * @param oldVersion the deprecated version
     * @param newVersion the replacement version
     */
    public void deprecateVersion(String oldVersion, String newVersion) {
        deprecatedVersions.put(oldVersion, newVersion);
        log.warn("API version '{}' is deprecated. Please migrate to '{}'", oldVersion, newVersion);
    }

    /**
     * Returns the current active version for a given API path.
     * This is used by controllers to determine which version to serve.
     *
     * @param request the incoming HTTP request
     * @return the active API version string
     */
    public String getActiveVersion(HttpServletRequest request) {
        return resolveVersion(request);
    }
}