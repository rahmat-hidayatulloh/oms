package com.oms.product.controller;

import java.util.List;
import java.util.UUID;

import com.oms.config.annotation.ApiVersion;
import com.oms.product.dto.ProductFilterRequest;
import com.oms.product.dto.ProductRequest;
import com.oms.product.dto.ProductResponse;
import com.oms.ratelimit.RateLimit;
import com.oms.product.service.ProductServiceInterface;
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
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * REST endpoints for managing products (v1).
 * Supports CRUD operations and dynamic search with filtering, sorting, and
 * pagination.
 * <p>
 * All responses return data directly (RFC 7807 ProblemDetail for errors).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@ApiVersion("v1")
@RequiredArgsConstructor
@Tag(name = "Product", description = "Product CRUD and search. ADMIN: create/update/delete; CUSTOMER: view/search.")
@RateLimit(name = "products")
public class ProductController {

    private final ProductServiceInterface productService;

    @Operation(
        summary = "Create a new product",
        description = "Creates a new product. Returns 400 if product name already exists.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created successfully",
            content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error or duplicate product name"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
        summary = "Get all products",
        description = "Returns a list of all active (non-deleted) products.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of products retrieved successfully",
            content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.findAll());
    }

    @Operation(
        summary = "Get product by ID",
        description = "Returns a single product by UUID. Returns 404 if not found or soft-deleted.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product found",
            content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ProductResponse> getProductById(
        @Parameter(description = "Product UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable UUID id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @Operation(
        summary = "Search products with filters",
        description = "Dynamic search with filters, sorting, and pagination.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search results with pagination metadata",
            content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "400", description = "Invalid filter parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
        @Valid ProductFilterRequest filter) {
        return ResponseEntity.ok(productService.search(filter));
    }

    @Operation(
        summary = "Update an existing product",
        description = "Updates a product by UUID. Returns 400 if new name already exists.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product updated successfully",
            content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error or duplicate product name"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
        @Parameter(description = "Product UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable UUID id,
        @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @Operation(
        summary = "Soft delete a product",
        description = "Soft-deletes a product by setting deleted_at timestamp.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product soft-deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(
        @Parameter(description = "Product UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.ok().build();
    }
}
