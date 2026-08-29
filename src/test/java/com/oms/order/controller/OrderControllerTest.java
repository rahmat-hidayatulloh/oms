package com.oms.order.controller;

import com.oms.order.dto.OrderRequest;
import com.oms.order.dto.OrderResponse;
import com.oms.order.entity.OrderStatus;
import com.oms.order.OrderConstants;
import com.oms.exception.ResourceNotFoundException;
import com.oms.order.service.OrderServiceInterface;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderControllerTest {

    @Mock
    private OrderServiceInterface orderService;

    @InjectMocks
    private OrderController orderController;

    @Test
    void createOrder_shouldReturn201() {
        UUID userId = UUID.randomUUID();
        setupAuthentication("testuser", "ROLE_CUSTOMER");

        OrderRequest request = new OrderRequest(Collections.emptyList());
        OrderResponse response = new OrderResponse(
                UUID.randomUUID(), "ORD-001", userId,
                new BigDecimal("100000.00"), OrderStatus.PENDING,
                Collections.emptyList(), null, null);
        when(orderService.createOrder(any(OrderRequest.class), any())).thenReturn(response);

        ResponseEntity<OrderResponse> result = orderController.createOrder(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().orderNumber()).isEqualTo("ORD-001");
    }

    @Test
    void getAllOrders_shouldReturn200WithPage() {
        setupAuthentication("admin", "ROLE_ADMIN");

        Page<OrderResponse> mockPage = new PageImpl<>(Collections.emptyList());
        when(orderService.findAll(eq(0), eq(20))).thenReturn(mockPage);

        ResponseEntity<Page<OrderResponse>> result =
                orderController.getAllOrders(0, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
    }

    @Test
    void getOrderById_adminShouldReturn200() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        setupAuthentication("admin", "ROLE_ADMIN");

        OrderResponse response = new OrderResponse(
                orderId, "ORD-001", userId,
                new BigDecimal("100000.00"), OrderStatus.PENDING,
                Collections.emptyList(), null, null);
        when(orderService.findByIdForUser(eq(orderId), eq("admin"), eq(true)))
                .thenReturn(response);

        ResponseEntity<OrderResponse> result =
                orderController.getOrderById(orderId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().orderNumber()).isEqualTo("ORD-001");
    }

    @Test
    void getOrderById_customerCanAccessOwnOrder() {
        UUID orderId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        setupAuthentication("customer1", "ROLE_CUSTOMER");

        OrderResponse response = new OrderResponse(
                orderId, "ORD-001", currentUserId,
                new BigDecimal("100000.00"), OrderStatus.PENDING,
                Collections.emptyList(), null, null);
        when(orderService.findByIdForUser(eq(orderId), eq("customer1"), eq(false)))
                .thenReturn(response);

        ResponseEntity<OrderResponse> result =
                orderController.getOrderById(orderId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().userId()).isEqualTo(currentUserId);
    }

    @Test
    void getOrderById_customerCannotAccessOtherUsersOrder() {
        UUID orderId = UUID.randomUUID();

        setupAuthentication("customer1", "ROLE_CUSTOMER");

        when(orderService.findByIdForUser(eq(orderId), eq("customer1"), eq(false)))
                .thenThrow(new ResourceNotFoundException(
                        OrderConstants.ENTITY_ORDER, "id", orderId));

        assertThatThrownBy(() -> orderController.getOrderById(orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteOrder_adminShouldReturn204() {
        UUID orderId = UUID.randomUUID();
        setupAuthentication("admin", "ROLE_ADMIN");

        doNothing().when(orderService)
                .deleteForUser(eq(orderId), eq("admin"), eq(true));

        ResponseEntity<Void> result =
                orderController.deleteOrder(orderId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private void setupAuthentication(String username, String role) {
        org.springframework.security.core.Authentication authentication =
                mock(org.springframework.security.core.Authentication.class);
        org.mockito.Mockito.lenient()
                .when(authentication.getName()).thenReturn(username);
        org.springframework.security.core.GrantedAuthority authority =
                new org.springframework.security.core.authority
                        .SimpleGrantedAuthority(role);
        org.mockito.Mockito.lenient()
                .doReturn(Collections.singletonList(authority))
                .when(authentication).getAuthorities();

        org.springframework.security.core.context.SecurityContext ctx =
                mock(org.springframework.security.core
                        .context.SecurityContext.class);
        org.mockito.Mockito.lenient()
                .when(ctx.getAuthentication()).thenReturn(authentication);
        org.springframework.security.core.context
                .SecurityContextHolder.setContext(ctx);
    }
}
