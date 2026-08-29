package com.oms.order.repository;

import com.oms.product.repository.ProductRepository;

import com.oms.config.AuditorAwareConfig;
import com.oms.product.entity.Category;
import com.oms.order.entity.Order;
import com.oms.order.entity.OrderItem;
import com.oms.order.entity.OrderStatus;
import com.oms.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository test for {@link OrderRepository}.
 * <p>
 * Testing is necessary here because {@link OrderRepository#findByIdWithItems(UUID)}
 * uses a custom JPQL {@code @Query} with {@code LEFT JOIN FETCH}.
 * Unlike derived queries from Spring Data JPA, custom JPQL queries
 * should be verified to ensure they execute correctly.
 * <p>
 * {@link DataJpaTest} loads only JPA-related beans (not full application context),
 * making tests faster and more focused.
 */
@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
@Import(AuditorAwareConfig.class)
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        product1 = productRepository.save(Product.builder()
                .name("Product A")
                .price(new BigDecimal("10000.00"))
                .stock(100)
                .category(Category.ELECTRONICS)
                .build());

        product2 = productRepository.save(Product.builder()
                .name("Product B")
                .price(new BigDecimal("20000.00"))
                .stock(100)
                .category(Category.BOOKS)
                .build());
    }

    private Order createOrderWithItems(String orderNumber, UUID userId, int itemCount) {
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(userId)
                .totalAmount(new BigDecimal("50000.00"))
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = entityManager.persistAndFlush(order);

        for (int i = 0; i < itemCount; i++) {
            Product product = (i % 2 == 0) ? product1 : product2;
            OrderItem item = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(i + 1)
                    .price(product.getPrice())
                    .order(savedOrder)
                    .build();
            entityManager.persistAndFlush(item);
        }

        // Clear persistence context to force real DB query on read
        entityManager.clear();
        return savedOrder;
    }

    @Test
    void findByIdWithItems_shouldReturnOrderWithItems() {
        UUID userId = UUID.randomUUID();
        Order savedOrder = createOrderWithItems("OMS-001", userId, 3);

        Optional<Order> result = orderRepository.findByIdWithItems(savedOrder.getId());

        assertThat(result).isPresent();
        Order order = result.get();
        assertThat(order.getOrderNumber()).isEqualTo("OMS-001");
        assertThat(order.getUserId()).isEqualTo(userId);
        assertThat(order.getOrderItems()).hasSize(3);
    }

    @Test
    void findByIdWithItems_shouldReturnEmptyWhenOrderNotFound() {
        Optional<Order> result = orderRepository.findByIdWithItems(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void findByIdWithItems_shouldReturnOrderWithCorrectItemDetails() {
        UUID userId = UUID.randomUUID();
        Order savedOrder = createOrderWithItems("OMS-002", userId, 2);

        Order order = orderRepository.findByIdWithItems(savedOrder.getId()).orElseThrow();

        List<OrderItem> items = order.getOrderItems();
        assertThat(items).hasSize(2);

        OrderItem item1 = items.get(0);
        assertThat(item1.getProductId()).isEqualTo(product1.getId());
        assertThat(item1.getProductName()).isEqualTo("Product A");
        assertThat(item1.getQuantity()).isEqualTo(1);

        OrderItem item2 = items.get(1);
        assertThat(item2.getProductId()).isEqualTo(product2.getId());
        assertThat(item2.getProductName()).isEqualTo("Product B");
        assertThat(item2.getQuantity()).isEqualTo(2);
    }

    @Test
    void findByIdWithItems_shouldReturnOrderEvenWhenNoItems() {
        UUID userId = UUID.randomUUID();
        Order order = Order.builder()
                .orderNumber("OMS-EMPTY")
                .userId(userId)
                .totalAmount(BigDecimal.ZERO)
                .status(OrderStatus.PENDING)
                .build();
        Order savedOrder = entityManager.persistAndFlush(order);
        entityManager.clear();

        Optional<Order> result = orderRepository.findByIdWithItems(savedOrder.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getOrderItems()).isEmpty();
    }

    @Test
    void findAll_withEntityGraph_shouldEagerlyFetchItems() {
        UUID userId = UUID.randomUUID();
        createOrderWithItems("OMS-003", userId, 2);
        createOrderWithItems("OMS-004", userId, 1);

        List<Order> orders = orderRepository.findAll();

        assertThat(orders).hasSize(2);

        // Access items — should NOT throw LazyInitializationException
        // because @EntityGraph eagerly loads them
        for (Order order : orders) {
            assertThat(order.getOrderItems()).isNotEmpty();
        }
    }
}