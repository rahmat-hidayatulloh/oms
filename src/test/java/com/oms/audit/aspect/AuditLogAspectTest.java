package com.oms.audit.aspect;

import com.oms.audit.annotation.Auditable;
import com.oms.audit.service.AuditLogService;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

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

    @Test
    void auditMethod_shouldRecordEntityIdFromRecordAccessor() throws Throwable {
        Auditable auditable = createAuditableAnnotation("APPROVE", "Order");
        UUID orderId = UUID.randomUUID();
        when(joinPoint.proceed()).thenReturn(new OrderIdRecord(orderId));
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(String.class);
        when(methodSignature.getName()).thenReturn("approve");

        auditLogAspect.auditMethod(joinPoint, auditable);

        ArgumentCaptor<AuditLogService.AuditEvent> captor =
                ArgumentCaptor.forClass(AuditLogService.AuditEvent.class);
        verify(auditLogService).record(captor.capture());
        // Records expose `id()` (not `getId()`); the aspect must resolve the real
        // identifier instead of falling back to the object hash code.
        assertThat(captor.getValue().entityId()).isEqualTo(orderId.toString());
        assertThat(captor.getValue().action()).isEqualTo("APPROVE");
        assertThat(captor.getValue().entityType()).isEqualTo("Order");
    }

    @Test
    void auditMethod_shouldRecordEntityIdFromJavaBeanGetter() throws Throwable {
        Auditable auditable = createAuditableAnnotation("UPDATE", "Product");
        when(joinPoint.proceed()).thenReturn(new BeanWithId("product-42"));
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn(String.class);
        when(methodSignature.getName()).thenReturn("update");

        auditLogAspect.auditMethod(joinPoint, auditable);

        ArgumentCaptor<AuditLogService.AuditEvent> captor =
                ArgumentCaptor.forClass(AuditLogService.AuditEvent.class);
        verify(auditLogService).record(captor.capture());
        assertThat(captor.getValue().entityId()).isEqualTo("product-42");
    }

    /** Minimal record exposing an {@code id()} accessor, like the domain responses. */
    private record OrderIdRecord(UUID id) {
    }

    /** Minimal JavaBean exposing a {@code getId()} getter. */
    private static final class BeanWithId {

        private final String id;

        private BeanWithId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
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
