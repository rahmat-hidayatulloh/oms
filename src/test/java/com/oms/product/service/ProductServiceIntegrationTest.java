package com.oms.product.service;

import com.oms.config.AbstractIntegrationTest;
import com.oms.product.dto.ProductRequest;
import com.oms.product.dto.ProductResponse;
import com.oms.product.entity.Category;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for ProductService using Testcontainers (PostgreSQL).
 * <p>
 * This test verifies the complete product lifecycle:
 * 1. Create product
 * 2. Find all products
 * 3. Find product by ID
 * 4. Update product
 * 5. Delete product (soft delete)
 * <p>
 * Uses real PostgreSQL database via Testcontainers.
 * <p>
 * <b>Note:</b> Requires Docker to be running. Set DOCKER_AVAILABLE=true to enable.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "DOCKER_AVAILABLE", matches = "true")
class ProductServiceIntegrationTest extends AbstractIntegrationTest {

    @BeforeAll
    static void checkDocker() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                "true".equals(System.getenv("DOCKER_AVAILABLE")),
                "Docker is not available. Skipping Testcontainers integration tests."
        );
    }

    @Autowired
    private ProductService productService;

    @Test
    void createProduct_shouldPersistToDatabase() {
        ProductRequest request = new ProductRequest(
                "Test Product",
                "Test Description",
                new BigDecimal("100000"),
                50,
                Category.ELECTRONICS
        );

        ProductResponse response = productService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Test Product");
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(response.stock()).isEqualTo(50);
        assertThat(response.category()).isEqualTo(Category.ELECTRONICS);
    }

    @Test
    void findAll_shouldReturnAllProducts() {
        createTestProduct("Product 1", Category.CLOTHING);
        createTestProduct("Product 2", Category.ELECTRONICS);
        createTestProduct("Product 3", Category.BOOKS);

        List<ProductResponse> products = productService.findAll();

        assertThat(products).hasSize(3);
        assertThat(products).extracting(ProductResponse::name)
                .containsExactlyInAnyOrder("Product 1", "Product 2", "Product 3");
    }

    @Test
    void findById_shouldReturnProduct() {
        ProductResponse created = createTestProduct("Test Product", Category.FOOD_AND_BEVERAGE);

        ProductResponse found = productService.findById(created.id());

        assertThat(found).isNotNull();
        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.name()).isEqualTo("Test Product");
        assertThat(found.category()).isEqualTo(Category.FOOD_AND_BEVERAGE);
    }

    @Test
    void updateProduct_shouldUpdateInDatabase() {
        ProductResponse created = createTestProduct("Original Name", Category.CLOTHING);

        ProductRequest updateRequest = new ProductRequest(
                "Updated Name",
                "Updated Description",
                new BigDecimal("150000"),
                100,
                Category.ELECTRONICS
        );

        ProductResponse updated = productService.update(created.id(), updateRequest);

        assertThat(updated).isNotNull();
        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.name()).isEqualTo("Updated Name");
        assertThat(updated.price()).isEqualByComparingTo(new BigDecimal("150000"));
        assertThat(updated.stock()).isEqualTo(100);
        assertThat(updated.category()).isEqualTo(Category.ELECTRONICS);
    }

    @Test
    void deleteProduct_shouldSoftDelete() {
        ProductResponse created = createTestProduct("To Delete", Category.FOOD_AND_BEVERAGE);

        productService.delete(created.id());

        List<ProductResponse> products = productService.findAll();
        assertThat(products).isEmpty();
    }

    /**
     * Helper method to create a test product.
     */
    private ProductResponse createTestProduct(String name, Category category) {
        ProductRequest request = new ProductRequest(
                name,
                "Description for " + name,
                new BigDecimal("50000"),
                25,
                category
        );
        return productService.create(request);
    }
}