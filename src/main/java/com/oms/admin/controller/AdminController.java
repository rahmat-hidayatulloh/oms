package com.oms.admin.controller;

import java.util.Map;

import com.oms.config.annotation.ApiVersion;
import com.oms.admin.dto.UserResponse;
import com.oms.ratelimit.RateLimit;
import com.oms.admin.service.AdminDashboardServiceInterface;
import com.oms.admin.service.AdminUserManagementServiceInterface;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin-only endpoints for dashboard statistics and user management (v1).
 * All endpoints require ADMIN role.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@ApiVersion("v1")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin dashboard and user management. All endpoints require ADMIN role.")
@RateLimit(name = "admin")
public class AdminController {

    private final AdminDashboardServiceInterface dashboardService;
    private final AdminUserManagementServiceInterface userManagementService;

    @Operation(
        summary = "Get admin dashboard statistics",
        description = "Returns dashboard statistics: total users, products, orders, revenue.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboard statistics",
            content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")
    })
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = dashboardService.getDashboardStats();
        return ResponseEntity.ok(dashboard);
    }

    @Operation(
        summary = "Get all users",
        description = "Returns a page of registered users with details.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of users",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")
    })
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort) {

        Sort.Direction direction = Sort.Direction.DESC;
        String property = "createdAt";
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.trim().split(",");
            property = parts[0];
            if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1])) {
                direction = Sort.Direction.ASC;
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, property));
        Page<UserResponse> users = userManagementService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }
}
