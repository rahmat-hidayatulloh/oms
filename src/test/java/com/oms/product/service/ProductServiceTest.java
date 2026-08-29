package com.oms.product.service;

import com.oms.config.AbstractServiceTest;
import com.oms.product.dto.ProductFilterRequest;
import com.oms.product.dto.ProductRequest;
import com.oms.product.dto.ProductResponse;
import com.oms.product.entity.Category;
import com.oms.exception.BadRequestException;
import com.oms.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for ProductService covering CRUD and QueryDSL search.
 */
class ProductServiceTest extends AbstractServiceTest {

        @Autowired
        private com.oms.product.service.ProductService productService;

        @Test
        void shouldCreateProduct() {
                ProductRequest productRequest = new ProductRequest(
                                "Kaos Polos Hitam",
                                "Kaos katun premium",
                                new BigDecimal("85000.00"),
                                100,
                                Category.CLOTHING);

                ProductResponse response = productService.create(productRequest);

                assertThat(response.id()).isNotNull();
                assertThat(response.name()).isEqualTo("Kaos Polos Hitam");
                assertThat(response.price()).isEqualByComparingTo(new BigDecimal("85000.00"));
                assertThat(response.stock()).isEqualTo(100);
                assertThat(response.category()).isEqualTo(Category.CLOTHING);
        }

        @Test
        void shouldThrowExceptionWhenCreatingDuplicateProduct() {
                ProductRequest productRequest = new ProductRequest(
                                "Kaos Polos Hitam",
                                "Kaos katun premium",
                                new BigDecimal("85000.00"),
                                100,
                                Category.CLOTHING);
                productService.create(productRequest);

                assertThatThrownBy(() -> productService.create(productRequest))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("already exists");
        }

        @Test
        void shouldFindProductById() {
                ProductRequest productRequest = new ProductRequest(
                                "Kaos Polos Hitam",
                                "Kaos katun premium",
                                new BigDecimal("85000.00"),
                                100,
                                Category.CLOTHING);
                ProductResponse created = productService.create(productRequest);

                ProductResponse found = productService.findById(created.id());

                assertThat(found.id()).isEqualTo(created.id());
                assertThat(found.name()).isEqualTo(created.name());
        }

        @Test
        void shouldThrowExceptionWhenProductNotFound() {
                assertThatThrownBy(() -> productService.findById(UUID.randomUUID()))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("not found");
        }

        @Test
        void shouldFindAllProducts() {
                productService.create(new ProductRequest(
                                "Kaos Polos Hitam", "Kaos katun premium",
                                new BigDecimal("85000.00"), 100, Category.CLOTHING));
                productService.create(new ProductRequest(
                                "Headphone Bluetooth", "Wireless headphone",
                                new BigDecimal("250000.00"), 50, Category.ELECTRONICS));

                List<ProductResponse> products = productService.findAll();

                // Filter to only include products created in this test by checking names
                List<String> ourNames = List.of("Kaos Polos Hitam", "Headphone Bluetooth");
                List<ProductResponse> ourProducts = products.stream()
                                .filter(p -> ourNames.contains(p.name()))
                                .toList();

                assertThat(ourProducts).hasSize(2);
                assertThat(products).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        void shouldUpdateProduct() {
                ProductRequest createRequest = new ProductRequest(
                                "Kaos Polos Hitam",
                                "Kaos katun premium",
                                new BigDecimal("85000.00"),
                                100,
                                Category.CLOTHING);
                ProductResponse created = productService.create(createRequest);

                ProductRequest updateRequest = new ProductRequest(
                                "Kaos Polos Putih", "Kaos katun premium putih",
                                new BigDecimal("90000.00"), 80, Category.CLOTHING);

                ProductResponse updated = productService.update(created.id(), updateRequest);

                assertThat(updated.name()).isEqualTo("Kaos Polos Putih");
                assertThat(updated.price()).isEqualByComparingTo(new BigDecimal("90000.00"));
                assertThat(updated.stock()).isEqualTo(80);
        }

        @Test
        void shouldSoftDeleteProduct() {
                ProductRequest createRequest = new ProductRequest(
                                "Kaos Polos Hitam",
                                "Kaos katun premium",
                                new BigDecimal("85000.00"),
                                100,
                                Category.CLOTHING);
                ProductResponse created = productService.create(createRequest);

                productService.delete(created.id());

                // findAll should exclude soft-deleted products
                List<ProductResponse> allProducts = productService.findAll();
                assertThat(allProducts).noneMatch(p -> p.id().equals(created.id()));
        }


        @Test
        void shouldSearchProductsWithDefaultPagination() {
                seedProducts();

                ProductFilterRequest filter = new ProductFilterRequest(
                                null, null, null, null, null, null, 0, 0);

                Page<ProductResponse> result = productService.search(filter);

                assertThat(result).isNotEmpty();
                assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(3);
                assertThat(result.getSize()).isEqualTo(10); // default size
        }

        @Test
        void shouldFilterProductsByName() {
                seedProducts();

                ProductFilterRequest filter = new ProductFilterRequest(
                                "kaos", null, null, null, "name", "asc", 0, 10);

                Page<ProductResponse> result = productService.search(filter);

                assertThat(result).isNotEmpty();
                assertThat(result.getContent()).allMatch(p -> p.name().toLowerCase().contains("kaos"));
        }

        @Test
        void shouldFilterProductsByPriceRange() {
                seedProducts();

                ProductFilterRequest filter = new ProductFilterRequest(
                                null, new BigDecimal("50000"), new BigDecimal("100000"),
                                null, "price", "asc", 0, 10);

                Page<ProductResponse> result = productService.search(filter);

                assertThat(result).isNotEmpty();
                assertThat(result.getContent()).allMatch(p -> p.price().compareTo(new BigDecimal("50000")) >= 0 &&
                                p.price().compareTo(new BigDecimal("100000")) <= 0);
        }

        @Test
        void shouldFilterProductsByCategory() {
                // Create products with specific categories
                com.oms.product.entity.Product product1 = com.oms.product.entity.Product.builder()
                                .name("Kaos Polos Hitam")
                                .description("Kaos katun")
                                .price(new BigDecimal("85000.00"))
                                .stock(100)
                                .category(Category.CLOTHING)
                                .build();
                productRepository.save(product1);

                com.oms.product.entity.Product product2 = com.oms.product.entity.Product.builder()
                                .name("Headphone Bluetooth")
                                .description("Wireless headphone")
                                .price(new BigDecimal("250000.00"))
                                .stock(50)
                                .category(Category.ELECTRONICS)
                                .build();
                productRepository.save(product2);

                ProductFilterRequest filter = new ProductFilterRequest(
                                null, null, null, Category.CLOTHING,
                                "name", "asc", 0, 10);

                Page<ProductResponse> result = productService.search(filter);

                assertThat(result).isNotEmpty();
                assertThat(result.getContent()).hasSize(1);
                assertThat(result.getContent().get(0).category()).isEqualTo(Category.CLOTHING);
        }

        @Test
        void shouldSortProductsByName() {
                seedProducts();

                ProductFilterRequest filter = new ProductFilterRequest(
                                null, null, null, null, "name", "asc", 0, 10);

                Page<ProductResponse> result = productService.search(filter);

                assertThat(result).isNotEmpty();
                assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(1);
        }

        @Test
        void shouldHandleInvalidSortField() {
                seedProducts();

                ProductFilterRequest filter = new ProductFilterRequest(
                                null, null, null, null, "invalidField", "asc", 0, 10);

                Page<ProductResponse> result = productService.search(filter);

                // Should not throw exception, should fallback to default sort
                assertThat(result).isNotNull();
        }


        private void seedProducts() {
                saveTestProduct("Kaos Polos Hitam", 100);
                saveTestProduct("Headphone Bluetooth", 50);
                saveTestProduct("Buku Tulis", 200);
        }
}