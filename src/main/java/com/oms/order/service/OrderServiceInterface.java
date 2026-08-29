package com.oms.order.service;

import com.oms.order.dto.OrderRequest;
import com.oms.order.dto.OrderResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Interface for order business operations.
 * Controllers depend on this abstraction (DIP) rather than the concrete
 * implementation. IDOR protection is handled at the service layer (SOLID D-2).
 */
public interface OrderServiceInterface {

    OrderResponse createOrder(OrderRequest request, UUID userId);

    OrderResponse findById(UUID id);

    /**
     * Finds an order with IDOR protection.
     * Non-admin users can only access their own orders.
     *
     * @param id       the order ID
     * @param username the requesting user's username
     * @param isAdmin  whether the user has ADMIN role
     * @return the order response
     */
    OrderResponse findByIdForUser(UUID id, String username, boolean isAdmin);

    Page<OrderResponse> findAll(int page, int size);

    void delete(UUID id);

    /**
     * Deletes an order with IDOR protection.
     * Non-admin users can only delete their own orders.
     *
     * @param id       the order ID
     * @param username the requesting user's username
     * @param isAdmin  whether the user has ADMIN role
     */
    void deleteForUser(UUID id, String username, boolean isAdmin);
}
