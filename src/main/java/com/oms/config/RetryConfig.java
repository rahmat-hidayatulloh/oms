package com.oms.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;

/**
 * Retry configuration using programmatic RetryTemplate for dynamic
 * configuration.
 * Replaces the annotation-based @Retryable which cannot use dynamic values.
 */
@Configuration
@EnableRetry
@RequiredArgsConstructor
public class RetryConfig {

    private final RetryProperties retryProperties;

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(retryProperties.getBackoffDelayMs());
        backOffPolicy.setMultiplier(retryProperties.getBackoffMultiplier());
        backOffPolicy.setMaxInterval(retryProperties.getMaxBackoffDelayMs());
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Retry both the raw JPA OptimisticLockException and its Spring-translated
        // ObjectOptimisticLockingFailureException (a subclass of
        // OptimisticLockingFailureException). Because order creation runs inside a
        // Spring-managed TransactionTemplate, Hibernate conflicts surface as the
        // Spring data-access exception; mapping the base class catches both.
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                retryProperties.getMaxAttempts(),
                Map.of(
                        OptimisticLockingFailureException.class, true,
                        OptimisticLockException.class, true));
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    /**
     * Programmatic transaction template. Used by {@link com.oms.order.service.OrderService}
     * to run each retry attempt inside its own independent transaction, so that an
     * optimistic-lock conflict raised at commit time is observed and retried by the
     * surrounding {@code RetryTemplate} instead of being lost at the outer boundary.
     *
     * @param transactionManager the active platform transaction manager
     * @return a self-contained transaction template for one unit of work
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}