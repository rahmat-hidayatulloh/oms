package com.oms.order.entity;

import com.oms.product.entity.Category;
import com.oms.product.entity.Product;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTest {

    @Test
    void testOrderCreation() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-001")
                .userId(UUID.randomUUID())
                .totalAmount(new BigDecimal("100.00"))
                .status(OrderStatus.PENDING)
                .build();

        assertNotNull(order.getId());
        assertEquals(orderId, order.getId());
        assertEquals("ORD-001", order.getOrderNumber());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertTrue(order.getOrderItems().isEmpty());
    }

    @Test
    void testAddOrderItem() {
        Order order = Order.builder()
                .orderNumber("ORD-002")
                .userId(UUID.randomUUID())
                .totalAmount(new BigDecimal("200.00"))
                .build();

        Product product = Product.builder()
                .name("Test Product")
                .price(new BigDecimal("50.00"))
                .stock(10)
                .category(Category.OTHER)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .productId(product.getId())
                .productName(product.getName())
                .quantity(2)
                .price(product.getPrice())
                .build();

        order.addOrderItem(orderItem);

        assertEquals(1, order.getOrderItems().size());
        assertEquals(order, orderItem.getOrder());
    }

    @Test
    void testRemoveOrderItem() {
        Order order = Order.builder()
                .orderNumber("ORD-003")
                .userId(UUID.randomUUID())
                .totalAmount(new BigDecimal("150.00"))
                .build();

        OrderItem orderItem = OrderItem.builder()
                .productId(UUID.randomUUID())
                .productName("Product")
                .quantity(1)
                .price(new BigDecimal("150.00"))
                .build();

        order.addOrderItem(orderItem);
        assertEquals(1, order.getOrderItems().size());

        order.removeOrderItem(orderItem);
        assertEquals(0, order.getOrderItems().size());
        assertNull(orderItem.getOrder());
    }

    @Test
    void testOrderItemsUnmodifiable() {
        Order order = Order.builder()
                .orderNumber("ORD-004")
                .userId(UUID.randomUUID())
                .totalAmount(new BigDecimal("100.00"))
                .build();

        OrderItem orderItem = OrderItem.builder()
                .productId(UUID.randomUUID())
                .productName("Product")
                .quantity(1)
                .price(new BigDecimal("100.00"))
                .build();

        order.addOrderItem(orderItem);

        assertThrows(UnsupportedOperationException.class, () -> {
            order.getOrderItems().add(orderItem);
        });
    }
}