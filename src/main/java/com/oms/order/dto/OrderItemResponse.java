package com.oms.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response payload representing a single order line item.
 */
public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productName,
        Integer quantity,
        BigDecimal price
) {
}