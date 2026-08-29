package com.oms.audit.service;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for JPA auditing functionality.
 * <p>
 * Verifies that:
 * <ul>
 *   <li>{@code @CreatedDate} and {@code @CreatedBy} are set on create</li>
 *   <li>{@code @LastModifiedDate} and {@code @LastModifiedBy} are set on update</li>
 *   <li>Auditor is retrieved from SecurityContext (or "system" as default)</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditingIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .name("Test Product")
                .price(new BigDecimal("50000.00"))
                .stock(100)
                .category(Category.ELECTRONICS)
                .build();
    }

    @Test
    void shouldSetCreatedByAndCreatedAtOnCreate() {
        Product saved = productRepository.save(product);

        assertThat(saved.getCreatedBy()).isEqualTo("system");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldSetUpdatedByAndUpdatedAtOnUpdate() {
        Product saved = productRepository.save(product);
        assertThat(saved.getCreatedBy()).isEqualTo("system");

        saved.setName("Updated Product Name");
        Product updated = productRepository.save(saved);

        assertThat(updated.getUpdatedBy()).isEqualTo("system");
        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(updated.getCreatedAt());
    }

    @Test
    void shouldNotChangeCreatedByAndCreatedAtOnUpdate() {
        Product saved = productRepository.save(product);
        String originalCreatedBy = saved.getCreatedBy();
        var originalCreatedAt = saved.getCreatedAt();

        saved.setPrice(new BigDecimal("60000.00"));
        Product updated = productRepository.save(saved);

        assertThat(updated.getCreatedBy()).isEqualTo(originalCreatedBy);
        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
    }
}