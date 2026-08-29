package com.oms.product.repository;

import com.oms.product.dto.ProductFilterRequest;
import com.oms.product.entity.Category;
import com.oms.product.entity.QProduct;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Utility class for building QueryDSL {@link Predicate} instances
 * used to dynamically filter {@link com.oms.product.entity.Product} entities.
 * <p>
 * Supports filtering by:
 * <ul>
 *   <li>name — case-insensitive contains</li>
 *   <li>price — min/max range</li>
 *   <li>category — exact match</li>
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProductPredicates {

    private static final QProduct Q_PRODUCT = QProduct.product;

    private static final int MAX_NAME_LENGTH = 100;

    private static final Pattern UNSAFE_NAME_PATTERN = Pattern.compile(
            "[\\u0000-\\u001F\\u007F]|--|;|/\\*|\\*/|xp_|exec|"
                    + "union|select|insert|update|delete|drop|alter|create|truncate",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Builds a composite {@link Predicate} from the given filter request.
     * Only non-null filter values are included in the predicate.
     * Returns {@code null} if no filters are active (meaning "no filtering").
     *
     * @param filter the filter parameters
     * @return a combined Predicate, or null if no filters are specified
     */
    public static Predicate build(ProductFilterRequest filter) {
        BooleanBuilder builder = new BooleanBuilder();

        if (filter.name() != null && !filter.name().isBlank()) {
            builder.and(withNameContaining(sanitizeName(filter.name().trim())));
        }

        if (filter.minPrice() != null) {
            builder.and(withMinPrice(filter.minPrice()));
        }

        if (filter.maxPrice() != null) {
            builder.and(withMaxPrice(filter.maxPrice()));
        }

        if (filter.category() != null) {
            builder.and(withCategory(filter.category()));
        }

        return builder.getValue();
    }

    /**
     * Sanitizes product name input by enforcing max length and rejecting unsafe characters/patterns.
     *
     * @param name raw name input
     * @return sanitized name
     * @throws IllegalArgumentException if input contains unsafe patterns or is too long
     */
    public static String sanitizeName(String name) {
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Product search name must be at most " + MAX_NAME_LENGTH + " characters.");
        }
        if (UNSAFE_NAME_PATTERN.matcher(name).find()) {
            throw new IllegalArgumentException(
                    "Product search name contains invalid characters or patterns.");
        }
        return name;
    }

    /**
     * Filter: product name contains the given string (case-insensitive).
     */
    public static BooleanExpression withNameContaining(String name) {
        return Q_PRODUCT.name.containsIgnoreCase(name);
    }

    /**
     * Filter: product price is greater than or equal to the given minimum.
     */
    public static BooleanExpression withMinPrice(BigDecimal minPrice) {
        return Q_PRODUCT.price.goe(minPrice);
    }

    /**
     * Filter: product price is less than or equal to the given maximum.
     */
    public static BooleanExpression withMaxPrice(BigDecimal maxPrice) {
        return Q_PRODUCT.price.loe(maxPrice);
    }

    /**
     * Filter: product category equals the given category.
     */
    public static BooleanExpression withCategory(Category category) {
        return Q_PRODUCT.category.eq(category);
    }
}
