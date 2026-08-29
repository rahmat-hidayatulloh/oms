package com.oms.audit.aspect;

import com.oms.audit.annotation.Auditable;
import com.oms.audit.service.AuditLogService;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AOP aspect that intercepts methods annotated with {@link Auditable}
 * and records audit log entries.
 * <p>
 * The aspect extracts the action and entity type from the annotation,
 * attempts to resolve the current user from the security context,
 * and delegates to {@link AuditLogService} for persistence.
 *
 * @see Auditable
 * @see AuditLogService
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    /**
     * Intercepts methods annotated with {@link Auditable} and records
     * an audit log entry after successful execution.
     *
     * @param joinPoint the intercepted method call
     * @param auditable the audit annotation metadata
     * @return the original method result
     * @throws Throwable if the intercepted method throws
     */
    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint,
                              Auditable auditable) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            String userId = resolveUserId();
            String entityId = resolveEntityId(result);
            String ipAddress = resolveIpAddress();

            auditLogService.record(new AuditLogService.AuditEvent(
                    auditable.action(),
                    auditable.entityType(),
                    entityId,
                    userId,
                    ipAddress,
                    buildDetails(joinPoint)
            ));
        } catch (Exception e) {
            log.warn("Failed to record audit log: {}", e.getMessage());
        }

        return result;
    }

    private String resolveUserId() {
        try {
            return org.springframework.security.core.context
                    .SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getName();
        } catch (Exception e) {
            return "system";
        }
    }

    private String resolveEntityId(Object result) {
        if (result == null) {
            return null;
        }
        try {
            Method getId = result.getClass().getMethod("getId");
            Object id = getId.invoke(result);
            return id != null ? id.toString() : null;
        } catch (NoSuchMethodException | IllegalAccessException
                 | java.lang.reflect.InvocationTargetException e) {
            return String.valueOf(result.hashCode());
        }
    }

    private String resolveIpAddress() {
        ServletRequestAttributes attrs =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }

    private String buildDetails(ProceedingJoinPoint joinPoint) {
        MethodSignature signature =
            (MethodSignature) joinPoint.getSignature();
        return String.format("method=%s.%s",
                signature.getDeclaringType().getSimpleName(),
                signature.getName());
    }
}
