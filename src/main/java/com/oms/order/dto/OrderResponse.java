package com.oms.order.dto;

import com.oms.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response payload representing an Order, including its line items.
 */
public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID userId,
        BigDecimal totalAmount,
        OrderStatus status,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}