package com.oms.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Email notification service with async support.
 * Email sending is simulated via logging (no real SMTP).
 */
@Slf4j
@Service
public class EmailService implements EmailServiceInterface {

    @Override
    @Async("emailExecutor")
    public void sendOrderConfirmationEmail(String to, String orderNumber, BigDecimal totalAmount, UUID userId) {
        log.info("=== SIMULATED EMAIL ===");
        log.info("To      : {}", to);
        log.info("Subject : Order Confirmation {}", orderNumber);
        log.info("Body    :");
        log.info("Order {} has been created successfully.", orderNumber);
        log.info("Total   : Rp {}", totalAmount);
        log.info("User ID : {}", userId);
        log.info("=== END SIMULATED EMAIL ===");
    }
}
