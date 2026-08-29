package com.oms.audit.aspect;

import com.oms.audit.annotation.Auditable;
import com.oms.audit.service.AuditLogService;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuditLogAspect}.
 * Verifies that the aspect intercepts @Auditable methods and
 * records audit log entries.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogAspectTest {

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuditLogAspect auditLogAspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Test
    void auditMethod_shouldRecordAuditLog() throws Throwable {
        Auditable auditable = createAuditableAnnotation("CREATE", "Product");
        when(joinPoint.proceed()).thenReturn("result");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(String.class);
        when(methodSignature.getName()).thenReturn("testMethod");

        Object result = auditLogAspect.auditMethod(joinPoint, auditable);

        assertThat(result).isEqualTo("result");
        verify(auditLogService).record(
                org.mockito.ArgumentMatchers.any(
                    AuditLogService.AuditEvent.class));
    }

    @Test
    void auditMethod_shouldReturnOriginalResult() throws Throwable {
        Auditable auditable = createAuditableAnnotation("UPDATE", "Order");
        String expectedResult = "orderResponse";
        when(joinPoint.proceed()).thenReturn(expectedResult);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(String.class);
        when(methodSignature.getName()).thenReturn("updateOrder");

        Object result = auditLogAspect.auditMethod(joinPoint, auditable);

        assertThat(result).isEqualTo(expectedResult);
    }

    private Auditable createAuditableAnnotation(
            String action, String entityType) {
        return new Auditable() {
            @Override
            public Class<? extends java.lang.annotation.Annotation>
                    annotationType() {
                return Auditable.class;
            }

            @Override
            public String action() {
                return action;
            }

            @Override
            public String entityType() {
                return entityType;
            }
        };
    }
}
