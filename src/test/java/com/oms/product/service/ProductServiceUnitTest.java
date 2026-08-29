package com.oms.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.oms.product.dto.ProductFilterRequest;
import com.oms.product.dto.ProductRequest;
import com.oms.product.dto.ProductResponse;
import com.oms.product.entity.Category;
import com.oms.product.entity.Product;
import com.oms.exception.BadRequestException;
import com.oms.exception.ResourceNotFoundException;
import com.oms.product.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.support.RetryTemplate;

/**
 * Unit tests for ProductService using Mockito.
 * Tests business logic without loading Spring context.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceUnitTest {

        @Mock
        private ProductRepository productRepository;

        @Mock
        private RetryTemplate retryTemplate;

        @InjectMocks
        private ProductService productService;

        private ProductRequest productRequest;
        private Product product;

        @BeforeEach
        void setUp() {
                org.mockito.Mockito.lenient()
                                .when(retryTemplate.execute(any(org.springframework.retry.RetryCallback.class)))
                                .thenAnswer(inv -> inv.getArgument(0, org.springframework.retry.RetryCallback.class)
                                                .doWithRetry(null));
                productRequest = new ProductRequest(
                                "Kaos Polos Hitam",
                                "Kaos katun premium",
                                new BigDecimal("85000.00"),
                                100,
                                Category.CLOTHING);

                product = Product.builder()
                                .id(UUID.randomUUID())
                                .name("Kaos Polos Hitam")
                                .description("Kaos katun premium")
                                .price(new BigDecimal("85000.00"))
                                .stock(100)
                                .category(Category.CLOTHING)
                                .build();
        }


        @Test
        void create_shouldSaveProduct_whenNameIsUnique() {
                when(productRepository.existsByName("Kaos Polos Hitam")).thenReturn(false);
                when(productRepository.save(any(Product.class))).thenReturn(product);

                ProductResponse response = productService.create(productRequest);

                assertThat(response).isNotNull();
                assertThat(response.id()).isEqualTo(product.getId());
                assertThat(response.name()).isEqualTo("Kaos Polos Hitam");
                assertThat(response.price()).isEqualByComparingTo(new BigDecimal("85000.00"));

                verify(productRepository).existsByName("Kaos Polos Hitam");
                verify(productRepository).save(any(Product.class));
        }

        @Test
        void create_shouldThrowException_whenNameAlreadyExists() {
                when(productRepository.existsByName("Kaos Polos Hitam")).thenReturn(true);

                assertThatThrownBy(() -> productService.create(productRequest))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("already exists");

                verify(productRepository, never()).save(any(Product.class));
        }


        @Test
        void findById_shouldReturnProduct_whenExists() {
                UUID productId = product.getId();
                when(productRepository.findById(productId)).thenReturn(Optional.of(product));

                ProductResponse response = productService.findById(productId);

                assertThat(response).isNotNull();
                assertThat(response.id()).isEqualTo(productId);
                assertThat(response.name()).isEqualTo("Kaos Polos Hitam");

                verify(productRepository).findById(productId);
        }

        @Test
        void findById_shouldThrowException_whenNotFound() {
                UUID randomId = UUID.randomUUID();
                when(productRepository.findById(randomId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> productService.findById(randomId))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("not found");

                verify(productRepository).findById(randomId);
        }


        @Test
        void findAll_shouldReturnAllProducts() {
                Product product2 = Product.builder()
                                .id(UUID.randomUUID())
                                .name("Headphone Bluetooth")
                                .price(new BigDecimal("250000.00"))
                                .stock(50)
                                .category(Category.ELECTRONICS)
                                .build();

                when(productRepository.findAll()).thenReturn(List.of(product, product2));

                List<ProductResponse> responses = productService.findAll();

                assertThat(responses).hasSize(2);
                assertThat(responses).extracting(ProductResponse::name)
                                .containsExactlyInAnyOrder("Kaos Polos Hitam", "Headphone Bluetooth");

                verify(productRepository).findAll();
        }


        @Test
        void update_shouldUpdateProduct_whenNameIsUnique() {
                UUID productId = product.getId();
                when(productRepository.findById(productId)).thenReturn(Optional.of(product));
                when(productRepository.existsByName("Kaos Polos Putih")).thenReturn(false);
                when(productRepository.save(any(Product.class))).thenReturn(product);

                ProductRequest updateRequest = new ProductRequest(
                                "Kaos Polos Putih",
                                "Kaos katun premium putih",
                                new BigDecimal("90000.00"),
                                80,
                                Category.CLOTHING);

                ProductResponse response = productService.update(productId, updateRequest);

                assertThat(response).isNotNull();
                verify(productRepository).findById(productId);
                verify(productRepository).save(any(Product.class));
        }

        @Test
        void update_shouldThrowException_whenNewNameAlreadyExists() {
                UUID productId = product.getId();
                when(productRepository.findById(productId)).thenReturn(Optional.of(product));
                when(productRepository.existsByName("Kaos Polos Putih")).thenReturn(true);

                ProductRequest updateRequest = new ProductRequest(
                                "Kaos Polos Putih",
                                "Kaos katun premium putih",
                                new BigDecimal("90000.00"),
                                80,
                                Category.CLOTHING);

                assertThatThrownBy(() -> productService.update(productId, updateRequest))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("already exists");

                verify(productRepository, never()).save(any(Product.class));
        }


        @Test
        void delete_shouldCallRepositoryDeleteById() {
                UUID productId = product.getId();
                when(productRepository.findById(productId)).thenReturn(java.util.Optional.of(product));

                productService.delete(productId);

                verify(productRepository).findById(productId);
                verify(productRepository).deleteById(productId);
        }


        @Test
        void search_shouldReturnPageOfProducts() {
                ProductFilterRequest filter = new ProductFilterRequest(
                                null, null, null, null, null, null, 0, 10);

                Page<Product> productPage = new PageImpl<>(List.of(product));
                when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);

                Page<ProductResponse> result = productService.search(filter);

                assertThat(result).isNotNull();
                assertThat(result.getContent()).hasSize(1);
                assertThat(result.getContent().get(0).name()).isEqualTo("Kaos Polos Hitam");

                verify(productRepository).findAll(any(Pageable.class));
        }

        @Test
        void search_shouldReturnEmptyPage_whenNoProductsMatch() {
                ProductFilterRequest filter = new ProductFilterRequest(
                                "nonexistent", null, null, null, null, null, 0, 10);

                Page<Product> emptyPage = new PageImpl<>(List.of());
                when(productRepository.findAll(any(com.querydsl.core.types.Predicate.class), any(Pageable.class)))
                                .thenReturn(emptyPage);

                Page<ProductResponse> result = productService.search(filter);

                assertThat(result).isEmpty();
                assertThat(result.getTotalElements()).isZero();
        }


        @ParameterizedTest
        @EnumSource(Category.class)
        void create_shouldWorkWithAllCategories(Category category) {
                ProductRequest request = new ProductRequest(
                                "Test Product",
                                "Description",
                                new BigDecimal("50000.00"),
                                25,
                                category);

                Product savedProduct = Product.builder()
                                .id(UUID.randomUUID())
                                .name("Test Product")
                                .category(category)
                                .build();

                when(productRepository.existsByName("Test Product")).thenReturn(false);
                when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

                ProductResponse response = productService.create(request);

                assertThat(response).isNotNull();
                assertThat(response.category()).isEqualTo(category);
        }
}