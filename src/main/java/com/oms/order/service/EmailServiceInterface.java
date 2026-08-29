package com.oms.order.service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Interface for email notification operations.
 * Consumers depend on this abstraction (DIP) rather than the concrete implementation.
 */
public interface EmailServiceInterface {

    void sendOrderConfirmationEmail(String to, String orderNumber,
                                     BigDecimal totalAmount, UUID userId);
}
