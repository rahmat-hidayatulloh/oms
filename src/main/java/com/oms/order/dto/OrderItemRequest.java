package com.oms.order.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request payload for a single line item in an order.
 */
public record OrderItemRequest(
        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotNull(message = "Quantity is required")
        Integer quantity
) {
}