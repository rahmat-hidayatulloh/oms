package com.oms.product.service;

import com.oms.product.dto.ProductFilterRequest;
import com.oms.product.dto.ProductRequest;
import com.oms.product.dto.ProductResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

/**
 * Interface for product business operations.
 * Controllers depend on this abstraction (DIP) rather than the concrete implementation.
 */
public interface ProductServiceInterface {

    ProductResponse create(ProductRequest request);

    ProductResponse findById(UUID id);

    List<ProductResponse> findAll();

    Page<ProductResponse> search(ProductFilterRequest filter);

    ProductResponse update(UUID id, ProductRequest request);

    void delete(UUID id);
}
