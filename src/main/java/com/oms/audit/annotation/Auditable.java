package com.oms.audit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for automatic audit logging via AOP.
 * <p>
 * When applied to a public method of a Spring-managed bean, the
 * {@link com.oms.audit.aspect.AuditLogAspect} intercepts the call and
 * records an audit log entry in the {@code audit_log} table.
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * &#64;Auditable(action = "CREATE", entityType = "Order")
 * public OrderResponse createOrder(OrderRequest request) { ... }
 * }</pre>
 *
 * @see com.oms.audit.aspect.AuditLogAspect
 * @see com.oms.audit.service.AuditLogService
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * The action performed (e.g., CREATE, UPDATE, DELETE, LOGIN).
     *
     * @return the action string
     */
    String action();

    /**
     * The entity type affected (e.g., Product, Order, User).
     *
     * @return the entity type string
     */
    String entityType();
}
