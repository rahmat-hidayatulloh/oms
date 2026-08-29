package com.oms.product.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.oms.product.entity.Category;

/**
 * Response payload representing a Product.
 */
public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        Category category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) implements Serializable {
    private static final long serialVersionUID = 1L;
}