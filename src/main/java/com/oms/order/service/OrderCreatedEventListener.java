package com.oms.order.service;

import com.oms.order.service.OrderService.OrderCreatedEvent;


import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener for order created events.
 * Handles sending order confirmation emails asynchronously after transaction commits.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventListener {
    

    private final EmailServiceInterface emailService;

    /**
     * Handles order created events and sends confirmation email.
     * This method is executed asynchronously after the transaction commits.
     *
     * @param event the order created event
     */
    @Async
    @EventListener
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Sending order confirmation email for order {}", event.orderNumber());
        try {
            emailService.sendOrderConfirmationEmail(
                    event.userEmail(),
                    event.orderNumber(),
                    event.totalAmount(),
                    event.userId()
            );
            log.info("Order confirmation email sent successfully for order {}", event.orderNumber());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order {}", event.orderNumber(), e);
        }
    }
}
