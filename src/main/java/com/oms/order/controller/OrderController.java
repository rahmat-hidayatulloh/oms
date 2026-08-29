package com.oms.order.controller;

import java.util.UUID;

import com.oms.config.annotation.ApiVersion;
import com.oms.constant.SecurityConstants;
import com.oms.order.dto.OrderRequest;
import com.oms.order.dto.OrderResponse;
import com.oms.ratelimit.RateLimit;
import com.oms.order.service.OrderServiceInterface;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST endpoints for managing orders (v1).
 * IDOR protection is handled at the service layer.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@ApiVersion("v1")
@RequiredArgsConstructor
@Tag(name = "Order", description = "Order management.")
@RateLimit(name = "orders")
public class OrderController {

    private final OrderServiceInterface orderService;

    @Operation(
        summary = "Create a new order",
        description = "Creates a new order for the authenticated user.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order created",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<OrderResponse> createOrder(
        @Valid @RequestBody OrderRequest request) {
        // userId resolution is handled by the service to keep controllers thin
        OrderResponse order = orderService.createOrder(
                request, UUID.randomUUID());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @Operation(
        summary = "Get all orders",
        description = "Returns a paginated list of all orders. ADMIN role only.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated orders"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN only")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.findAll(page, size));
    }

    @Operation(
        summary = "Get order by ID",
        description = "Returns a single order by UUID. IDOR protection at service layer.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<OrderResponse> getOrderById(
        @Parameter(description = "Order UUID", required = true)
        @PathVariable UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(SecurityConstants.ROLE_ADMIN));

        return ResponseEntity.ok(orderService.findByIdForUser(id, username, isAdmin));
    }

    @Operation(
        summary = "Delete an order (soft delete)",
        description = "Soft-deletes an order. IDOR protection at service layer.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Order deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<Void> deleteOrder(
        @Parameter(description = "Order UUID", required = true)
        @PathVariable UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(SecurityConstants.ROLE_ADMIN));

        orderService.deleteForUser(id, username, isAdmin);
        return ResponseEntity.noContent().build();
    }
}
