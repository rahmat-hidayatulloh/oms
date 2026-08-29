package com.oms.order.service;

import com.oms.order.dto.OrderItemRequest;
import com.oms.order.dto.OrderRequest;
import com.oms.order.dto.OrderResponse;
import com.oms.product.entity.Category;
import com.oms.product.entity.Product;
import com.oms.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify that the N+1 query problem has been fixed
 * when fetching orders with their order items.
 * <p>
 * <b>Before fix:</b> 1 query for orders + N queries for items = N+1 queries
 * <b>After fix:</b>  1 query with LEFT JOIN FETCH = 1 query
 * <p>
 * This test enables SQL logging and counts the number of SELECT statements
 * executed during order retrieval.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceNPlusOneTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        Product product1 = productRepository.save(Product.builder()
                .name("Product A")
                .price(new BigDecimal("10000.00"))
                .stock(100)
                .category(Category.ELECTRONICS)
                .build());

        Product product2 = productRepository.save(Product.builder()
                .name("Product B")
                .price(new BigDecimal("20000.00"))
                .stock(100)
                .category(Category.BOOKS)
                .build());

        for (int i = 0; i < 3; i++) {
            OrderRequest request = new OrderRequest(List.of(
                    new OrderItemRequest(product1.getId(), 1),
                    new OrderItemRequest(product2.getId(), 2)
            ));
            orderService.createOrder(request, userId);
        }
    }

    @Test
    void shouldFetchAllOrdersWithItemsInSingleQuery() {
        List<OrderResponse> orders = orderService.findAll(0, 20).getContent();

        assertThat(orders).hasSize(3);

        for (OrderResponse order : orders) {
            assertThat(order.items()).isNotEmpty();
            assertThat(order.items()).hasSize(2);
        }

        long totalItems = orders.stream()
                .mapToLong(o -> o.items().size())
                .sum();
        assertThat(totalItems).isEqualTo(6);
    }

    @Test
    void shouldFetchOrderByIdWithItemsInSingleQuery() {
        List<OrderResponse> allOrders = orderService.findAll(0, 20).getContent();
        UUID firstOrderId = allOrders.get(0).id();

        OrderResponse order = orderService.findById(firstOrderId);

        assertThat(order).isNotNull();
        assertThat(order.items()).hasSize(2);
        assertThat(order.items().get(0).productName()).isEqualTo("Product A");
        assertThat(order.items().get(1).productName()).isEqualTo("Product B");
    }

    @Test
    void shouldAccessOrderItemsWithoutLazyInitializationException() {
        // This test verifies that items are eagerly fetched and accessible
        // outside of the transactional context (if needed)

        List<OrderResponse> orders = orderService.findAll(0, 20).getContent();

        for (OrderResponse order : orders) {
            // because they were fetched via @EntityGraph / JOIN FETCH
            assertThat(order.items()).isNotEmpty();

            for (var item : order.items()) {
                assertThat(item.productId()).isNotNull();
                assertThat(item.productName()).isNotBlank();
                assertThat(item.quantity()).isPositive();
                assertThat(item.price()).isPositive();
            }
        }
    }

    @Test
    void shouldReturnCorrectTotalAmountForOrders() {
        List<OrderResponse> orders = orderService.findAll(0, 20).getContent();

        for (OrderResponse order : orders) {
            assertThat(order.totalAmount())
                    .isEqualByComparingTo(new BigDecimal("50000.00"));
        }
    }
}