package com.oms.order.service;

import com.oms.auth.dto.LoginRequest;
import com.oms.auth.dto.LoginResponse;
import com.oms.order.dto.OrderItemRequest;
import com.oms.order.dto.OrderRequest;
import com.oms.order.dto.OrderResponse;
import com.oms.product.entity.Category;
import com.oms.order.entity.Order;
import com.oms.order.entity.OrderStatus;
import com.oms.product.entity.Product;
import com.oms.auth.entity.Role;
import com.oms.auth.entity.User;
import com.oms.exception.BadRequestException;
import com.oms.order.repository.OrderRepository;
import com.oms.product.repository.ProductRepository;
import com.oms.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * OMS-033: End-to-end integration test untuk Order Creation Flow.
 * <p>
 * Menggunakan Testcontainers (PostgreSQL dan Redis containers) dan
 * MockBean untuk EmailService (async email).
 * <p>
 * Flow yang diuji:
 * 1. Login sebagai CUSTOMER
 * 2. Create Order
 * 3. Verify stock berkurang
 * 4. Verify order tersimpan
 * 5. Verify async email terpanggil
 * 6. Test rollback jika stock tidak cukup
 * 7. Test concurrent order untuk product yang sama
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testcontainers")
@Testcontainers
@org.junit.jupiter.api.condition.DisabledIf("isDockerNotAvailable")
class OrderFlowIntegrationTest {

    private static boolean dockerAvailable;

    /** PostgreSQL container for integration testing. */
    @Container
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

    /** Redis container for integration testing. */
    @Container
    protected static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379)
                    .withReuse(true);

    static {
        checkDocker();

        // Only start containers if Docker is available
        if (dockerAvailable) {
            try {
                POSTGRES_CONTAINER.start();
                REDIS_CONTAINER.start();
            } catch (Exception e) {
                System.err.println("WARNING: Could not start Testcontainers: " + e.getMessage());
                dockerAvailable = false;
            }

            // Set system properties for Spring Boot to pick up
            if (dockerAvailable) {
                System.setProperty("spring.datasource.url",
                        "jdbc:postgresql://localhost:" + POSTGRES_CONTAINER.getMappedPort(5432) + "/testdb");
                System.setProperty("spring.datasource.username", "test");
                System.setProperty("spring.datasource.password", "test");
                System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");

                System.setProperty("spring.data.redis.host", "localhost");
                System.setProperty("spring.data.redis.port", String.valueOf(REDIS_CONTAINER.getMappedPort(6379)));
                System.setProperty("spring.data.redis.enabled", "true");
            }
        }
    }

    private static void checkDocker() {
        try {
            // Check if Docker is available by attempting to ping the Docker daemon
            Process process = new ProcessBuilder("docker", "info").start();
            dockerAvailable = process.waitFor() == 0;
        } catch (Exception e) {
            dockerAvailable = false;
        }
    }

    private static boolean isDockerNotAvailable() {
        return !dockerAvailable;
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrderService orderService;

    @MockBean
    private EmailService emailService;

    private String adminToken;
    private String customerToken;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Create customer user if not exists (admin is seeded by V5 migration)
        createTestCustomer();

        // Login sebagai ADMIN untuk setup product
        adminToken = loginAndGetToken("admin", "P@ssw0rd");

        // Login sebagai CUSTOMER untuk order
        customerToken = loginAndGetToken("customer", "Customer123!");

        // Setup test product dengan stock 100
        testProduct = createTestProduct("Test Product Order Flow", 100);
    }

    private void createTestCustomer() {
        if (!userRepository.existsByUsername("customer")) {
            User customer = User.builder()
                    .username("customer")
                    .email("customer@oms.local")
                    .password(passwordEncoder.encode("Customer123!"))
                    .role(Role.CUSTOMER)
                    .build();
            userRepository.save(customer);
        }
    }


    @Test
    void completeOrderFlow_shouldSucceed() {
        UUID productId = testProduct.getId();
        int initialStock = testProduct.getStock();

        OrderResponse orderResponse = createOrder(customerToken, productId, 5);

        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.orderNumber()).matches("OMS-\\d{8}-\\d{4}");
        assertThat(orderResponse.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(orderResponse.items()).hasSize(1);
        assertThat(orderResponse.items().get(0).quantity()).isEqualTo(5);
        assertThat(orderResponse.items().get(0).productId()).isEqualTo(productId);

        assertThat(orderResponse.totalAmount())
                .isEqualByComparingTo(new BigDecimal("425000.00"));

        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(initialStock - 5);

        Order savedOrder = orderRepository.findByIdWithItems(orderResponse.id()).orElseThrow();
        assertThat(savedOrder.getOrderNumber()).isEqualTo(orderResponse.orderNumber());
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(savedOrder.getOrderItems()).hasSize(1);

        verify(emailService, timeout(5000).times(1))
                .sendOrderConfirmationEmail(anyString(), anyString(), any(), any());
    }


    @Test
    void createOrder_shouldRollback_whenStockInsufficient() {
        UUID productId = testProduct.getId();
        int initialStock = testProduct.getStock();

        assertThatThrownBy(() -> createOrder(customerToken, productId, 200))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");

        Product productAfterRollback = productRepository.findById(productId).orElseThrow();
        assertThat(productAfterRollback.getStock())
                .as("Stock should remain unchanged after rollback")
                .isEqualTo(initialStock);
    }


    @Test
    void concurrentOrders_shouldNotExceedStock() throws Exception {
        UUID productId = testProduct.getId();
        int initialStock = testProduct.getStock();
        int threadCount = 10;
        int quantityPerOrder = 15; // 10 * 15 = 150 > 100, should fail some

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        // Barrier pattern: all workers ready → all start simultaneously → all done
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await(); // Wait for the start signal
                    String token = loginAndGetToken("customer", "Customer123!");
                    createOrder(token, productId, quantityPerOrder);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        // Wait for all workers to be ready, then start them simultaneously
        assertThat(ready.await(5, TimeUnit.SECONDS))
                .as("All workers should be ready within 5 seconds")
                .isTrue();
        start.countDown(); // Fire the starting gun
        assertThat(done.await(30, TimeUnit.SECONDS))
                .as("All workers should complete within 30 seconds")
                .isTrue();
        executor.shutdown();

        Product finalProduct = productRepository.findById(productId).orElseThrow();
        int totalOrdered = initialStock - finalProduct.getStock();
        assertThat(totalOrdered)
                .as("Total ordered items should not exceed initial stock")
                .isLessThanOrEqualTo(initialStock);

        assertThat(successCount.get())
                .as("At least some concurrent orders should succeed")
                .isGreaterThan(0);

        assertThat(successCount.get() * quantityPerOrder)
                .as("Total successful orders should not exceed available stock")
                .isLessThanOrEqualTo(initialStock);

        assertThat(finalProduct.getStock())
                .as("Remaining stock should equal initial stock minus ordered quantity")
                .isEqualTo(initialStock - totalOrdered);
        assertThat(finalProduct.getStock())
                .as("Stock should never go negative")
                .isGreaterThanOrEqualTo(0);
    }


    @Test
    void createOrder_withMultipleItems_shouldCalculateTotalCorrectly() {
        Product product2 = createTestProduct("Second Product", 50);
        Product product3 = createTestProduct("Third Product", 30);

        OrderRequest request = new OrderRequest(List.of(
                new OrderItemRequest(testProduct.getId(), 2), // 2 * 85000 = 170000
                new OrderItemRequest(product2.getId(), 3), // 3 * 85000 = 255000
                new OrderItemRequest(product3.getId(), 1) // 1 * 85000 = 85000
        ));

        OrderResponse response = createOrder(customerToken, request);

        BigDecimal expectedTotal = new BigDecimal("510000.00"); // 170000 + 255000 + 85000
        assertThat(response.totalAmount())
                .as("Total amount should be sum of all item subtotals")
                .isEqualByComparingTo(expectedTotal);

        assertThat(response.items()).hasSize(3);

        assertThat(productRepository.findById(testProduct.getId()).orElseThrow().getStock())
                .isEqualTo(98); // 100 - 2
        assertThat(productRepository.findById(product2.getId()).orElseThrow().getStock())
                .isEqualTo(47); // 50 - 3
        assertThat(productRepository.findById(product3.getId()).orElseThrow().getStock())
                .isEqualTo(29); // 30 - 1
    }


    @Test
    void createOrder_withoutAuth_shouldReturn403() {
        OrderRequest request = new OrderRequest(List.of(
                new OrderItemRequest(testProduct.getId(), 1)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<OrderRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }


    @Test
    void createOrder_withZeroQuantity_shouldThrowBadRequest() {
        UUID productId = testProduct.getId();

        assertThatThrownBy(() -> createOrder(customerToken, productId, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be positive");
    }


    @Test
    void createOrder_withNonExistentProduct_shouldThrowResourceNotFound() {
        UUID nonExistentProductId = UUID.randomUUID();

        assertThatThrownBy(() -> createOrder(customerToken, nonExistentProductId, 1))
                .isInstanceOf(com.oms.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Product");
    }


    private String loginAndGetToken(String username, String password) {
        LoginRequest loginRequest = new LoginRequest(username, password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LoginRequest> entity = new HttpEntity<>(loginRequest, headers);

        ResponseEntity<LoginResponse> response = restTemplate.exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                entity,
                LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotEmpty();

        return response.getBody().accessToken();
    }

    private Product createTestProduct(String name, int stock) {
        Product product = Product.builder()
                .name(name + " - " + UUID.randomUUID().toString().substring(0, 8))
                .description("Test product for integration test")
                .price(new BigDecimal("85000.00"))
                .stock(stock)
                .category(Category.ELECTRONICS)
                .build();
        return productRepository.save(product);
    }

    private OrderResponse createOrder(String token, UUID productId, int quantity) {
        OrderRequest request = new OrderRequest(List.of(
                new OrderItemRequest(productId, quantity)));
        return createOrder(token, request);
    }

    private OrderResponse createOrder(String token, OrderRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<OrderRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                entity,
                OrderResponse.class);

        if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
            return response.getBody();
        }

        if (response.getStatusCode() == HttpStatus.BAD_REQUEST) {
            throw new BadRequestException("Insufficient stock for product");
        }
        if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new com.oms.exception.ResourceNotFoundException("Product", "id", "unknown");
        }

        throw new RuntimeException("Unexpected response: " + response.getStatusCode());
    }

    // Helper methods convertMapToOrderResponse and convertItemMapToResponse removed
    // since OrderController now returns OrderResponse directly instead of
    // ApiResponse
}