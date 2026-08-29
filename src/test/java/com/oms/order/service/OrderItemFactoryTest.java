package com.oms.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.oms.order.dto.OrderItemRequest;
import com.oms.product.entity.Category;
import com.oms.order.entity.OrderItem;
import com.oms.product.entity.Product;
import com.oms.exception.BadRequestException;
import com.oms.exception.ResourceNotFoundException;
import com.oms.product.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderItemFactoryTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderItemFactory orderItemFactory;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(UUID.randomUUID())
                .name("Laptop")
                .price(new BigDecimal("1000.00"))
                .stock(50)
                .category(Category.ELECTRONICS)
                .build();
    }

    @Test
    void buildAndValidateItems_shouldReduceStock() {
        List<OrderItemRequest> requests = List.of(
                new OrderItemRequest(product.getId(), 5));

        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<OrderItem> items = orderItemFactory.buildAndValidateItems(requests);

        assertEquals(1, items.size());
        assertEquals(5, items.get(0).getQuantity());
        assertEquals(45, product.getStock());
    }

    @Test
    void buildAndValidateItems_shouldThrowWhenStockInsufficient() {
        List<OrderItemRequest> requests = List.of(
                new OrderItemRequest(product.getId(), 100));

        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> orderItemFactory.buildAndValidateItems(requests));
        assert ex.getMessage().contains("Insufficient stock");
    }

    @Test
    void buildAndValidateItems_shouldThrowWhenQuantityZero() {
        List<OrderItemRequest> requests = List.of(
                new OrderItemRequest(product.getId(), 0));

        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));

        assertThrows(BadRequestException.class,
                () -> orderItemFactory.buildAndValidateItems(requests));
    }

    @Test
    void buildAndValidateItems_shouldThrowWhenQuantityNegative() {
        List<OrderItemRequest> requests = List.of(
                new OrderItemRequest(product.getId(), -1));

        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));

        assertThrows(BadRequestException.class,
                () -> orderItemFactory.buildAndValidateItems(requests));
    }

    @Test
    void buildAndValidateItems_shouldThrowWhenProductNotFound() {
        UUID missingId = UUID.randomUUID();
        List<OrderItemRequest> requests = List.of(
                new OrderItemRequest(missingId, 1));

        when(productRepository.findAllById(anyList())).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> orderItemFactory.buildAndValidateItems(requests));
    }

    @Test
    void buildAndValidateItems_shouldHandleMultipleItems() {
        Product product2 = Product.builder()
                .id(UUID.randomUUID())
                .name("Mouse")
                .price(new BigDecimal("25.00"))
                .stock(20)
                .category(Category.ELECTRONICS)
                .build();

        List<OrderItemRequest> requests = List.of(
                new OrderItemRequest(product.getId(), 10),
                new OrderItemRequest(product2.getId(), 5));

        when(productRepository.findAllById(anyList()))
                .thenReturn(List.of(product, product2));
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<OrderItem> items = orderItemFactory.buildAndValidateItems(requests);

        assertEquals(2, items.size());
        assertEquals(40, product.getStock());
        assertEquals(15, product2.getStock());
    }

    @Test
    void buildAndValidateItems_shouldSucceedWhenStockExactlyEqualsQuantity() {
        List<OrderItemRequest> requests = List.of(
                new OrderItemRequest(product.getId(), 50));

        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<OrderItem> items = orderItemFactory.buildAndValidateItems(requests);

        assertEquals(1, items.size());
        assertEquals(0, product.getStock());
    }
}
