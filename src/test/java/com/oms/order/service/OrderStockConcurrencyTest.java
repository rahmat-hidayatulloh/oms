package com.oms.order.service;

import com.oms.order.dto.OrderItemRequest;
import com.oms.order.dto.OrderRequest;
import com.oms.product.entity.Category;
import com.oms.product.entity.Product;
import com.oms.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic concurrency test proving that concurrent order creation for the
 * same {@link Product} never oversells its stock.
 * <p>
 * Each worker issues one order for one unit against a product with limited stock.
 * Optimistic locking (@Version) + per-attempt {@link org.springframework.transaction.support.TransactionTemplate}
 * + retry (from RetryConfig) guarantee that only the available units are ever
 * committed, so the invariant {@code remainingStock == initialStock - successes}
 * with {@code remainingStock >= 0} must always hold.
 */
@SpringBootTest
@ActiveProfiles("test")
// Use a dedicated in-memory database so the committed orders/products created here
// do not leak into (or inherit from) other tests sharing the cached Spring context.
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:orderconcurrency;DB_CLOSE_DELAY=-1")
class OrderStockConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void concurrentOrders_shouldNeverOversellStock() throws Exception {
        int stock = 5;
        int workers = 8;
        Product product = productRepository.save(Product.builder()
                .name("Concurrent Order Product")
                .price(new BigDecimal("20000.00"))
                .stock(stock)
                .category(Category.ELECTRONICS)
                .build());
        UUID userId = UUID.randomUUID();

        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(workers);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        for (int i = 0; i < workers; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    orderService.createOrder(
                            new OrderRequest(List.of(new OrderItemRequest(product.getId(), 1))), userId);
                    success.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        // Start all workers simultaneously for maximum contention.
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        // were consumed, one order per successful unit.
        int remaining = productRepository.findById(product.getId()).orElseThrow().getStock();

        assertThat(remaining).isGreaterThanOrEqualTo(0);
        assertThat(success.get()).isLessThanOrEqualTo(stock);
        assertThat(remaining).isEqualTo(stock - success.get());
        assertThat(failed.get() + success.get()).isEqualTo(workers);
    }
}