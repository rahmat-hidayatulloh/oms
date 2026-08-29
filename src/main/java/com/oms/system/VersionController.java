package com.oms.system;

import com.oms.config.versioning.VersioningResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.oms.constant.OpenApiConstants;
import com.oms.order.OrderConstants;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * Controller that provides API version information.
 * <p>
 * Returns the current active version, available versions, and deprecation status.
 * This endpoint is unauthenticated and accessible at {@code GET /api/version}.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "System", description = "System endpoints for API version info, health checks, and utilities.")
public class VersionController {

    private final VersioningResolver versioningResolver;

    @Operation(
        summary = "Get API version information",
        description = "Returns current API version, available versions, and API docs URL. Public endpoint.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Version information retrieved successfully",
                content = @Content(schema = @Schema(implementation = Map.class)))
        })
    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> getVersionInfo(HttpServletRequest request) {
        String currentVersion = versioningResolver.resolveVersion(request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("application", "OMS");
        response.put(OpenApiConstants.VERSION_CURRENT, currentVersion);
        response.put(OpenApiConstants.VERSION_AVAILABLE, getAvailableVersions());
        response.put(OpenApiConstants.VERSION_DEPRECATED, getDeprecatedVersions());
        response.put(OpenApiConstants.VERSION_LATEST, "v1");
        response.put(OpenApiConstants.VERSION_API_DOCS, OpenApiConstants.SWAGGER_UI_PATH);
        response.put(OpenApiConstants.VERSION_SERVER_TIME, ZonedDateTime.now(ZoneId.of(OrderConstants.APP_TIMEZONE))
                .format(DateTimeFormatter.ISO_ZONED_DATE_TIME));

        return ResponseEntity.ok(response);
    }

    /**
     * Returns the list of all available API versions.
     */
    private List<Map<String, Object>> getAvailableVersions() {
        return List.of(
                createVersionInfo("v1", "Current stable version", false, null)
        );
    }

    /**
     * Returns the list of deprecated API versions.
     */
    private List<Map<String, Object>> getDeprecatedVersions() {
        return List.of();
    }

    /**
     * Creates a version info map.
     */
    private Map<String, Object> createVersionInfo(
            String version, String status, boolean deprecated, String replacement) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("version", version);
        info.put("status", status);
        info.put("deprecated", deprecated);
        if (replacement != null) {
            info.put("replacement", replacement);
        }
        return info;
    }
}