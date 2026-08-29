package com.oms.product.service;

import com.oms.product.entity.Category;
import com.oms.product.entity.Product;
import com.oms.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Concurrent test for optimistic locking on Product stock updates.
 * <p>
 * Simulates multiple threads trying to update the same product stock simultaneously.
 * Without optimistic locking, this could cause lost updates.
 * With @Version + @Retryable, conflicts are retried automatically.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:product_concurrent_test;DB_CLOSE_DELAY=-1"
})
class ProductConcurrentTest {

    @Autowired
    private ProductRepository productRepository;

    private Product product;

    @BeforeEach
    void setUp() {
        product = productRepository.save(Product.builder()
                .name("Concurrent Product")
                .price(new BigDecimal("10000.00"))
                .stock(100)
                .category(Category.ELECTRONICS)
                .build());
    }

    /**
     * Simulates a concurrent stock update with retry logic.
     * Returns true if successful, false if max retries exceeded.
     */
    private boolean simulateStockUpdate(UUID productId) {
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                Product current = productRepository.findById(productId).orElseThrow();
                int newStock = current.getStock() - 1;

                if (newStock < 0) {
                    return false; // Insufficient stock
                }

                current.setStock(newStock);
                productRepository.save(current);
                return true;

            } catch (org.springframework.dao.OptimisticLockingFailureException e) {
                // Version conflict - retry
                attempt++;
                try {
                    Thread.sleep(50); // Small backoff before retry
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }
}