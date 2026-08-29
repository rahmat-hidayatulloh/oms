package com.oms.product.dto;

import java.math.BigDecimal;

import com.oms.product.entity.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating or updating a Product.
 */
public record ProductRequest(
        @NotNull(message = "Product name is required")
        @Size(min = 3, max = 100, message = "Product name must be between 3 and 100 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "Stock is required")
        Integer stock,

        @NotNull(message = "Category is required")
        Category category
) {
}