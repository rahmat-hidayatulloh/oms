package com.oms.product.repository;

import com.oms.product.dto.ProductFilterRequest;
import com.oms.product.entity.Category;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductPredicatesTest {

    @Test
    void sanitizeName_shouldAcceptNormalInput() {
        String sanitized = ProductPredicates.sanitizeName("Kaos Polos");
        assertThat(sanitized).isEqualTo("Kaos Polos");
    }

    @Test
    void sanitizeName_shouldRejectTooLongInput() {
        String longName = "a".repeat(101);
        assertThatThrownBy(() -> ProductPredicates.sanitizeName(longName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most 100");
    }

    @Test
    void sanitizeName_shouldRejectUnsafePatterns() {
        assertThatThrownBy(() -> ProductPredicates.sanitizeName("test'; DROP TABLE product; --"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid characters or patterns");
    }

    @Test
    void build_shouldUseSanitizedName() {
        ProductFilterRequest filter = new ProductFilterRequest(
                "phone", null, null, Category.ELECTRONICS, "createdAt", "desc", 0, 10);
        assertThat(ProductPredicates.build(filter)).isNotNull();
    }
}
