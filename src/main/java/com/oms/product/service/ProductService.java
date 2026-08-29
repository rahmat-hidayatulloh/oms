package com.oms.product.service;

import java.util.List;
import java.util.UUID;

import com.oms.constant.CacheConstants;
import com.oms.constant.ErrorConstants;
import com.oms.order.OrderConstants;
import com.oms.product.dto.ProductFilterRequest;
import com.oms.product.dto.ProductRequest;
import com.oms.product.dto.ProductResponse;
import com.oms.product.entity.Product;
import com.oms.exception.BadRequestException;
import com.oms.exception.ResourceNotFoundException;
import com.oms.product.repository.ProductPredicates;
import com.oms.product.repository.ProductRepository;
import com.querydsl.core.types.Predicate;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Business logic for managing Product entities.
 * Supports dynamic filtering, sorting, and pagination via QueryDSL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService implements ProductServiceInterface {

    private final ProductRepository productRepository;
    private final org.springframework.retry.support.RetryTemplate retryTemplate;

    /**
     * Creates a new product after validating that its name is unique.
     * Updates the cache with the new product.
     */
    @Override
    @CachePut(value = CacheConstants.CACHE_PRODUCTS, key = "#result.id()")
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsByName(request.name())) {
            throw new BadRequestException(String.format(ErrorConstants.MSG_PRODUCT_NAME_EXISTS, request.name()));
        }

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .category(request.category())
                .build();

        Product saved = productRepository.save(product);
        log.info("Created product with id {}", saved.getId());
        return toResponse(saved);
    }

    /**
     * Returns a product by id or throws if not found.
     * Results are cached in Redis with key "products::{id}".
     */
    @Override
    @Cacheable(value = CacheConstants.CACHE_PRODUCTS, key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        return toResponse(findProductOrThrow(id));
    }

    /**
     * Returns all active (non-soft-deleted) products.
     * Results are cached in Redis with key "products_list::all".
     */
    @Override
    @Cacheable(value = CacheConstants.CACHE_PRODUCTS_LIST, key = "'all'")
    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Searches products with dynamic filtering, sorting, and pagination.
     *
     * @param filter the filter/sort/page parameters
     * @return a page of ProductResponse matching the criteria
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> search(ProductFilterRequest filter) {
        Predicate predicate = ProductPredicates.build(filter);
        Pageable pageable = createPageable(filter);

        Page<Product> productPage;

        if (predicate != null) {
            productPage = productRepository.findAll(predicate, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }

        log.debug("Search returned {} of {} products (page {})",
                productPage.getNumberOfElements(),
                productPage.getTotalElements(),
                filter.page());

        return productPage.map(this::toResponse);
    }

    /**
     * Updates an existing product. Name uniqueness is enforced, allowing the
     * product to keep its own current name.
     * <p>
     * Retries on {@link OptimisticLockException} up to 3 times with a 100ms
     * backoff.
     * This handles concurrent updates to the same product.
     * <p>
     * Evicts cache entries for the updated product and the products list.
     */
    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConstants.CACHE_PRODUCTS, key = "#id"),
        @CacheEvict(value = CacheConstants.CACHE_PRODUCTS_LIST, key = "'all'")
    })
    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        return retryTemplate.execute(context -> {
            Product product = findProductOrThrow(id);

            if (!product.getName().equals(request.name()) && productRepository.existsByName(request.name())) {
                throw new BadRequestException(String.format(ErrorConstants.MSG_PRODUCT_NAME_EXISTS, request.name()));
            }

            product.setName(request.name());
            product.setDescription(request.description());
            product.setPrice(request.price());
            product.setStock(request.stock());
            product.setCategory(request.category());

            Product updated = productRepository.save(product);
            log.info("Updated product with id {}", updated.getId());
            return toResponse(updated);
        });
    }

    /**
     * Soft-deletes a product by setting deleted_at timestamp.
     * Uses Hibernate {@code @SQLDelete} which overrides DELETE with UPDATE.
     * <p>
     * Evicts cache entries for the deleted product and the products list.
     */
    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConstants.CACHE_PRODUCTS, key = "#id"),
        @CacheEvict(value = CacheConstants.CACHE_PRODUCTS_LIST, key = "'all'")
    })
    @Transactional
    public void delete(UUID id) {
        findProductOrThrow(id);
        productRepository.deleteById(id);
        log.info("Soft-deleted product with id {}", id);
    }

    /**
     * Creates a {@link Pageable} from the filter request.
     * Validates the sort field against allowed values.
     */
    private Pageable createPageable(ProductFilterRequest filter) {
        String sortField = filter.sortBy();
        if (!CacheConstants.ALLOWED_SORT_FIELDS.contains(sortField)) {
            log.warn("Invalid sort field '{}', falling back to 'createdAt'", sortField);
            sortField = CacheConstants.SORT_CREATED_AT;
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(filter.sortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, sortField);
        return PageRequest.of(filter.page(), filter.size(), sort);
    }

    private Product findProductOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(OrderConstants.ENTITY_PRODUCT, "id", id));
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCategory(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
