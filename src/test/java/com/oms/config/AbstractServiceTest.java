package com.oms.config;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Base class for service tests providing common setup and utilities.
 * <p>
 * Reduces code duplication for common test data creation and setup.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractServiceTest {

    @Autowired
    protected com.oms.product.repository.ProductRepository productRepository;

    @Autowired
    protected com.oms.auth.repository.UserRepository userRepository;

    @Autowired
    protected com.oms.order.repository.OrderRepository orderRepository;

    @Autowired
    protected com.oms.product.service.ProductService productService;

    /**
     * Creates a test product with default values.
     *
     * @param name  the product name
     * @param stock the stock quantity
     * @return the created product entity
     */
    protected com.oms.product.entity.Product createTestProduct(String name, int stock) {
        return com.oms.product.entity.Product.builder()
                .name(name)
                .description("Test product: " + name)
                .price(BigDecimal.valueOf(85000))
                .stock(stock)
                .category(com.oms.product.entity.Category.ELECTRONICS)
                .build();
    }

    /**
     * Creates and saves a test product.
     *
     * @param name  the product name
     * @param stock the stock quantity
     * @return the saved product entity
     */
    protected com.oms.product.entity.Product saveTestProduct(String name, int stock) {
        com.oms.product.entity.Product product = createTestProduct(name, stock);
        return productRepository.save(product);
    }

    /**
     * Creates a test user with default values.
     *
     * @param username the username
     * @param role     the user role
     * @return the created user entity
     */
    protected com.oms.auth.entity.User createTestUser(String username, com.oms.auth.entity.Role role) {
        return com.oms.auth.entity.User.builder()
                .username(username)
                .email(username + "@oms.local")
                .password("encoded-password")
                .role(role)
                .build();
    }

    /**
     * Creates and saves a test user.
     *
     * @param username the username
     * @param role     the user role
     * @return the saved user entity
     */
    protected com.oms.auth.entity.User saveTestUser(String username, com.oms.auth.entity.Role role) {
        com.oms.auth.entity.User user = createTestUser(username, role);
        return userRepository.save(user);
    }

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }
}