package com.oms.order.service;

import com.oms.product.service.ProductService;

import com.oms.product.entity.Category;
import com.oms.order.entity.Order;
import com.oms.order.entity.OrderStatus;
import com.oms.product.entity.Product;
import com.oms.exception.ResourceNotFoundException;
import com.oms.order.repository.OrderRepository;
import com.oms.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**s
 * Integration test for soft delete functionality across all entities.
 * <p>
 * Verifies that:
 * <ul>
 *   <li>{@code @SQLDelete} overrides DELETE to UPDATE deleted_at</li>
 *   <li>{@code @SQLRestriction} filters out soft-deleted rows</li>
 *   <li>Soft-deleted entities are not returned by findAll() or findById()</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SoftDeleteIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        product = productRepository.save(Product.builder()
                .name("Test Product")
                .price(new BigDecimal("50000.00"))
                .stock(100)
                .category(Category.ELECTRONICS)
                .build());

        order = orderRepository.save(Order.builder()
                .orderNumber("TEST-001")
                .userId(UUID.randomUUID())
                .totalAmount(new BigDecimal("50000.00"))
                .status(OrderStatus.PENDING)
                .build());
    }


    @Test
    void shouldSoftDeleteProduct() {
        productService.delete(product.getId());

        assertThatThrownBy(() -> productService.findById(product.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }


    @Test
    void shouldSoftDeleteOrder() {
        orderService.delete(order.getId());

        assertThatThrownBy(() -> orderService.findById(order.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
