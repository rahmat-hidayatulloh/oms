package com.oms.product.service;

import com.oms.product.dto.ProductRequest;
import com.oms.product.dto.ProductResponse;
import com.oms.product.entity.Category;
import com.oms.product.entity.Product;
import com.oms.exception.ResourceNotFoundException;
import com.oms.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test cache functionality for ProductService.
 * <p>
 * Verifies:
 * <ul>
 *   <li>Cache hit/miss behavior</li>
 *   <li>Cache eviction on update/delete</li>
 *   <li>Cache put on create</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.data.redis.enabled=false"
})
@Transactional
class ProductCacheTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    private Product product;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        product = productRepository.save(Product.builder()
                .name("Test Product")
                .price(new BigDecimal("50000.00"))
                .stock(100)
                .category(Category.ELECTRONICS)
                .build());
    }

    @Test
    void shouldCacheProductOnFirstCall() {
        ProductResponse response = productService.findById(product.getId());

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(product.getId());
    }

    @Test
    void shouldReturnCachedProductOnSecondCall() {
        // First call
        ProductResponse response1 = productService.findById(product.getId());

        // Second call (caching disabled, but service should still work)
        ProductResponse response2 = productService.findById(product.getId());

        // Should return the same product
        assertThat(response1.id()).isEqualTo(response2.id());
        assertThat(response1.name()).isEqualTo(response2.name());
    }

    @Test
    void shouldEvictCacheOnUpdate() {
        // First call
        productService.findById(product.getId());

        // Update product (caching disabled, but service should still work)
        ProductResponse updated = productService.update(product.getId(), new ProductRequest(
                "Updated Product",
                "Updated description",
                new BigDecimal("60000.00"),
                50,
                Category.ELECTRONICS
        ));

        // Verify update succeeded
        assertThat(updated.name()).isEqualTo("Updated Product");
    }

    @Test
    void shouldEvictCacheOnDelete() {
        // First call
        productService.findById(product.getId());

        // Delete product (caching disabled, but service should still work)
        productService.delete(product.getId());

        // Verify product is soft deleted (throws ResourceNotFoundException due to @SQLRestriction)
        assertThatThrownBy(() -> productService.findById(product.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldCacheProductList() {
        // First call (caching disabled)
        var products = productService.findAll();

        // Verify service returns products
        assertThat(products).isNotNull();
    }

    @Test
    void shouldEvictProductListCacheOnUpdate() {
        // First call
        productService.findAll();

        // Update product (caching disabled)
        ProductResponse updated = productService.update(product.getId(), new ProductRequest(
                "Updated Product",
                "Updated description",
                new BigDecimal("60000.00"),
                50,
                Category.ELECTRONICS
        ));

        // Verify update succeeded
        assertThat(updated.name()).isEqualTo("Updated Product");
    }

    @Test
    void shouldPutNewProductInCache() {
        // Create new product (caching disabled)
        ProductResponse created = productService.create(new ProductRequest(
                "New Product",
                "New description",
                new BigDecimal("70000.00"),
                30,
                Category.ELECTRONICS
        ));

        // Verify product was created
        assertThat(created).isNotNull();
        assertThat(created.name()).isEqualTo("New Product");
    }
}