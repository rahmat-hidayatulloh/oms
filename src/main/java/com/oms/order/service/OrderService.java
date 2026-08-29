package com.oms.order.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.oms.order.dto.OrderItemResponse;
import com.oms.order.dto.OrderRequest;
import com.oms.order.dto.OrderResponse;
import com.oms.order.entity.Order;
import com.oms.order.entity.OrderItem;
import com.oms.order.entity.OrderStatus;
import com.oms.auth.entity.User;
import com.oms.order.repository.OrderRepository;
import com.oms.auth.repository.UserRepository;
import com.oms.order.OrderConstants;
import com.oms.exception.ResourceNotFoundException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Business logic for creating and querying orders with product stock validation
 * and reduction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService implements OrderServiceInterface {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderItemFactory orderItemFactory;
    private final EmailServiceInterface emailService;
    private final ApplicationEventPublisher eventPublisher;
    private final RetryTemplate retryTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Counter orderCreatedCounter;
    private final Counter orderFailedCounter;
    private final Timer orderProcessingTimer;

    /**
     * Creates an order for the given user, validating and reducing product stock
     * atomically.
     * <p>
     * Each retry attempt runs inside its own {@link TransactionTemplate} transaction
     * (which is <em>inside</em> the {@link RetryTemplate} callback). This is the
     * critical boundary fix: an optimistic-lock (\@Version) conflict on {@code Product}
     * is raised when the inner transaction commits, so it propagates to the surrounding
     * retry and is retried against a fresh read of committed stock.
     *
     * @param request the order request
     * @param userId  the id of the user placing the order
     * @return the created order
     */
    @Override
    public OrderResponse createOrder(OrderRequest request, UUID userId) {
        return orderProcessingTimer.record(() -> retryTemplate.execute(context -> {
            try {
                OrderResponse response = transactionTemplate.execute(status -> doCreateOrder(request, userId));
                orderCreatedCounter.increment();
                return response;
            } catch (Exception e) {
                orderFailedCounter.increment();
                throw e;
            }
        }));

    }

    private OrderResponse doCreateOrder(OrderRequest request, UUID userId) {
        List<OrderItem> orderItems = orderItemFactory.buildAndValidateItems(request.items());

        BigDecimal totalAmount = orderItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .build();

        for (OrderItem item : orderItems) {
            order.addOrderItem(item);
        }

        Order saved = orderRepository.save(order);

        log.info("Created order {} for user {}", saved.getOrderNumber(), userId);

        String userEmail = userRepository.findById(userId)
                .map(User::getEmail)
                .orElse(OrderConstants.DEFAULT_FALLBACK_EMAIL);

        eventPublisher.publishEvent(
                new OrderCreatedEvent(saved.getOrderNumber(), saved.getTotalAmount(), userId,
                        userEmail));

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException(OrderConstants.ENTITY_ORDER, "id", id));
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse findByIdForUser(UUID id, String username,
                                          boolean isAdmin) {
        OrderResponse order = findById(id);

        if (!isAdmin) {
            UUID currentUserId = userRepository.findByUsername(username)
                    .map(User::getId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            OrderConstants.ENTITY_USER, "username", username));

            if (!order.userId().equals(currentUserId)) {
                throw new ResourceNotFoundException(
                        OrderConstants.ENTITY_ORDER, "id", id);
            }
        }

        return order;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException(OrderConstants.ENTITY_ORDER, "id", id);
        }
        orderRepository.deleteById(id);
        log.info("Soft-deleted order with id {}", id);
    }

    @Override
    @Transactional
    public void deleteForUser(UUID id, String username, boolean isAdmin) {
        OrderResponse order = findById(id);

        if (!isAdmin) {
            UUID currentUserId = userRepository.findByUsername(username)
                    .map(User::getId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            OrderConstants.ENTITY_USER, "username", username));

            if (!order.userId().equals(currentUserId)) {
                throw new ResourceNotFoundException(
                        OrderConstants.ENTITY_ORDER, "id", id);
            }
        }

        orderRepository.deleteById(id);
        log.info("Soft-deleted order {} by user '{}'", id, username);
    }

    private String generateOrderNumber() {
        String datePart = LocalDate.now()
                .format(DateTimeFormatter.ofPattern(OrderConstants.ORDER_NUMBER_DATE_FORMAT));

        String randomPart = UUID.randomUUID()
                .toString()
                .substring(0, OrderConstants.ORDER_NUMBER_RANDOM_LENGTH)
                .toUpperCase(Locale.ROOT);

        return OrderConstants.ORDER_NUMBER_PREFIX
                + datePart
                + "-"
                + randomPart;
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPrice()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    /**
     * Event published after an order is created.
     */
    public record OrderCreatedEvent(String orderNumber, BigDecimal totalAmount, UUID userId, String userEmail) {
    }
}