package com.oms.product.repository;

import com.oms.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>,
        QuerydslPredicateExecutor<Product> {

    /**
     * Checks whether an active (non-soft-deleted) product with the given name exists.
     * The @SQLRestriction on Product already filters out soft-deleted rows.
     */
    boolean existsByName(String name);

    /**
     * Soft deletes a product by setting deleted_at timestamp.
     *
     * @param id the product UUID
     */
    @Modifying
    @Query("UPDATE Product p SET p.deletedAt = CURRENT_TIMESTAMP WHERE p.id = :id AND p.deletedAt IS NULL")
    void softDelete(UUID id);
}
