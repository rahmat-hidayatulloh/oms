package com.oms.product.dto;

import com.oms.product.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Request parameters for filtering, sorting, and paginating products via QueryDSL.
 */
@Schema(description = "Filter parameters for dynamic product search")
public record ProductFilterRequest(

        @Schema(description = "Search by product name (case-insensitive contain)", example = "kaos")
        String name,

        @Schema(description = "Minimum price filter", example = "10000")
        BigDecimal minPrice,

        @Schema(description = "Maximum price filter", example = "500000")
        BigDecimal maxPrice,

        @Schema(description = "Filter by product category", example = "ELECTRONICS")
        Category category,

        @Schema(description = "Sort field: name, price, createdAt", example = "createdAt")
        String sortBy,

        @Schema(description = "Sort direction: asc or desc", example = "desc")
        String sortDirection,

        @Schema(description = "Page number (0-based)", example = "0")
        int page,

        @Schema(description = "Page size", example = "20")
        int size
) {

    /**
     * Default constructor with sensible defaults.
     */
    public ProductFilterRequest {
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }
        if (sortDirection == null || sortDirection.isBlank()) {
            sortDirection = "desc";
        }
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 10;
        }
    }
}