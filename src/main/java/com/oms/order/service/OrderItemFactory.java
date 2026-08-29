package com.oms.order.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.oms.order.dto.OrderItemRequest;
import com.oms.order.entity.OrderItem;
import com.oms.product.entity.Product;
import com.oms.constant.ErrorConstants;
import com.oms.order.OrderConstants;
import com.oms.exception.BadRequestException;
import com.oms.exception.ResourceNotFoundException;
import com.oms.product.repository.ProductRepository;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Factory responsible for building and validating {@link OrderItem} instances
 * from {@link OrderItemRequest} inputs, including stock validation and reduction.
 */
@Component
@RequiredArgsConstructor
public class OrderItemFactory {

    private final ProductRepository productRepository;

    /**
     * Builds order items from request items, validating product existence,
     * quantity positivity, and stock availability. Reduces product stock
     * in-memory and persists all product changes via {@code saveAll}.
     *
     * @param itemRequests the requested order items
     * @return validated and built order items
     */
    public List<OrderItem> buildAndValidateItems(List<OrderItemRequest> itemRequests) {
        List<UUID> productIds = itemRequests.stream()
                .map(OrderItemRequest::productId)
                .toList();

        List<Product> products = productRepository.findAllById(productIds);
        Map<UUID, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : itemRequests) {
            Product product = productMap.get(itemRequest.productId());
            if (product == null) {
                throw new ResourceNotFoundException(
                        OrderConstants.ENTITY_PRODUCT, "id", itemRequest.productId());
            }

            int requested = itemRequest.quantity();
            if (requested <= 0) {
                throw new BadRequestException(
                        String.format(ErrorConstants.MSG_QUANTITY_POSITIVE, product.getName()));
            }

            if (product.getStock() < requested) {
                throw new BadRequestException(
                        String.format(ErrorConstants.MSG_INSUFFICIENT_STOCK,
                                product.getName(), requested, product.getStock()));
            }

            product.setStock(product.getStock() - requested);

            orderItems.add(OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(requested)
                    .price(product.getPrice())
                    .build());
        }

        productRepository.saveAll(products);

        return orderItems;
    }
}
