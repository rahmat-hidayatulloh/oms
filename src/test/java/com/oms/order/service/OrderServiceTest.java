package com.oms.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import com.oms.order.dto.OrderItemRequest;
import com.oms.order.dto.OrderRequest;
import com.oms.order.dto.OrderResponse;
import com.oms.order.entity.Order;
import com.oms.order.entity.OrderItem;
import com.oms.exception.BadRequestException;
import com.oms.exception.ResourceNotFoundException;
import com.oms.order.repository.OrderRepository;
import com.oms.auth.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemFactory orderItemFactory;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RetryTemplate retryTemplate;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private Counter orderCreatedCounter;

    @Mock
    private Counter orderFailedCounter;

    @Mock
    private Timer orderProcessingTimer;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient()
                .when(retryTemplate.execute(any(org.springframework.retry.RetryCallback.class)))
                .thenAnswer(inv -> inv.getArgument(0, org.springframework.retry.RetryCallback.class)
                        .doWithRetry(null));
        org.mockito.Mockito.lenient()
                .when(orderProcessingTimer.record(any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        org.mockito.Mockito.lenient()
                .when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(inv -> inv.getArgument(0, TransactionCallback.class).doInTransaction(null));
    }

    private OrderItem sampleOrderItem(UUID productId, String productName,
                                      int quantity, BigDecimal price) {
        return OrderItem.builder()
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .price(price)
                .build();
    }

    @Test
    void createOrder_shouldCreateOrderAndSendConfirmation() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        BigDecimal price = new BigDecimal("15000.00");
        OrderItem item = sampleOrderItem(productId, "Buku Tulis", 5, price);
        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(productId, 5)));

        when(orderItemFactory.buildAndValidateItems(anyList())).thenReturn(List.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        OrderResponse response = orderService.createOrder(request, userId);

        assertEquals(userId, response.userId());
        assertEquals(new BigDecimal("75000.00"), response.totalAmount());
        assertEquals(1, response.items().size());
        verify(orderItemFactory, times(1)).buildAndValidateItems(anyList());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void createOrder_shouldGenerateOrderNumberFormat() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderItem item = sampleOrderItem(productId, "Item", 2, new BigDecimal("50.00"));
        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(productId, 2)));

        when(orderItemFactory.buildAndValidateItems(anyList())).thenReturn(List.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        OrderResponse response = orderService.createOrder(request, userId);

        assertTrue(response.orderNumber().matches("OMS-\\d{8}-[A-F0-9]{8}"),
                "Order number should match OMS-YYYYMMDD-XXXXXXXX but was " + response.orderNumber());
    }

    @Test
    void createOrder_shouldThrowWhenProductNotFound() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(productId, 1)));

        when(orderItemFactory.buildAndValidateItems(anyList()))
                .thenThrow(new ResourceNotFoundException("Product", "id", productId));

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.createOrder(request, userId));
    }

    @Test
    void createOrder_shouldThrowWhenStockInsufficient() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(productId, 10)));

        when(orderItemFactory.buildAndValidateItems(anyList()))
                .thenThrow(new BadRequestException("Insufficient stock for product"));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> orderService.createOrder(request, userId));
        assertTrue(ex.getMessage().contains("Insufficient stock"));
    }

    @Test
    void createOrder_shouldThrowWhenQuantityNotPositive() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(productId, 0)));

        when(orderItemFactory.buildAndValidateItems(anyList()))
                .thenThrow(new BadRequestException("Quantity must be positive"));

        assertThrows(BadRequestException.class,
                () -> orderService.createOrder(request, userId));
    }

    @Test
    void createOrder_shouldThrowWhenQuantityNegative() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(productId, -1)));

        when(orderItemFactory.buildAndValidateItems(anyList()))
                .thenThrow(new BadRequestException("Quantity must be positive"));

        assertThrows(BadRequestException.class,
                () -> orderService.createOrder(request, userId));
    }

    @Test
    void createOrder_shouldHandleMultipleItems() {
        UUID userId = UUID.randomUUID();
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        OrderItem item1 = sampleOrderItem(productId1, "Item 1", 10, new BigDecimal("5000.00"));
        OrderItem item2 = sampleOrderItem(productId2, "Item 2", 15, new BigDecimal("20000.00"));
        OrderRequest request = new OrderRequest(List.of(
                new OrderItemRequest(productId1, 10),
                new OrderItemRequest(productId2, 15)));

        when(orderItemFactory.buildAndValidateItems(anyList())).thenReturn(List.of(item1, item2));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        OrderResponse response = orderService.createOrder(request, userId);

        assertEquals(2, response.items().size());
        assertEquals(new BigDecimal("350000.00"), response.totalAmount());
    }

    @Test
    void findAll_shouldReturnPageOfOrders() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(orderRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<OrderResponse> result = orderService.findAll(0, 20);

        assertTrue(result.getContent().isEmpty());
        verify(orderRepository, times(1)).findAll(any(PageRequest.class));
    }
}
