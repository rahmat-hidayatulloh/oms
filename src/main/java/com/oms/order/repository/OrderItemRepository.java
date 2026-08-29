package com.oms.order.repository;

import com.oms.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    /**
     * Soft deletes an order item by setting deleted_at timestamp.
     *
     * @param id the order item UUID
     */
    @Modifying
    @Query("UPDATE OrderItem oi SET oi.deletedAt = CURRENT_TIMESTAMP WHERE oi.id = :id AND oi.deletedAt IS NULL")
    void softDelete(UUID id);
}
