package com.oms.order.repository;

import com.oms.order.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Order} entities.
 * <p>
 * <b>N+1 Problem Fix:</b>
 * <p>
 * The {@code Order} entity has a {@code @OneToMany} relationship with {@code OrderItem}
 * which is fetched LAZILY by default. Without eager fetching, every call to
 * {@code order.getOrderItems()} triggers an additional SQL query.
 * <p>
 * <b>Before fix (N+1):</b>
 * <pre>
 * SELECT * FROM orders;                    -- 1 query
 * SELECT * FROM order_items WHERE order_id=?;  -- N queries (one per order)
 * </pre>
 * <b>After fix (1+1 → 1):</b>
 * <pre>
 * SELECT o.*, oi.* FROM orders o LEFT JOIN order_items oi ON oi.order_id = o.id;  -- 1 query
 * </pre>
 * <p>
 * Two approaches are provided:
 * <ul>
 *   <li>{@link #findAllWithItems()} — uses {@link EntityGraph} (type-safe, no raw SQL)</li>
 *   <li>{@link #findByIdWithItems(UUID)} — uses JPQL {@code LEFT JOIN FETCH} (explicit control)</li>
 * </ul>
 */
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Fetches all orders with their order items eagerly loaded in a single query
     * using {@link EntityGraph} with attribute path {@code orderItems}.
     * <p>
     * This eliminates the N+1 problem by generating a LEFT JOIN query.
     *
     * @return all orders with items eagerly loaded
     */
    @EntityGraph(attributePaths = {"orderItems"})
    @Override
    List<Order> findAll();

    /**
     * Fetches an order by ID with its order items eagerly loaded using JPQL JOIN FETCH.
     * <p>
     * This is an alternative to {@link EntityGraph} that provides explicit control
     * over the join strategy.
     *
     * @param id the order UUID
     * @return the order with items eagerly loaded, or empty if not found
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :id")
    Optional<Order> findByIdWithItems(UUID id);

    /**
     * Soft deletes an order by setting deleted_at timestamp.
     *
     * @param id the order UUID
     */
    @Modifying
    @Query("UPDATE Order o SET o.deletedAt = CURRENT_TIMESTAMP WHERE o.id = :id AND o.deletedAt IS NULL")
    void softDelete(UUID id);

    /**
     * Calculates total revenue from all non-deleted orders.
     *
     * @return the sum of all order total amounts, or zero if no orders exist
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.deletedAt IS NULL")
    BigDecimal calculateTotalRevenue();
}
