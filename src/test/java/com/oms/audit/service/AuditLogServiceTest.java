package com.oms.audit.service;

import com.oms.audit.entity.AuditLog;
import com.oms.audit.repository.AuditLogRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;

/**
 * Unit tests for {@link AuditLogService}.
 * Verifies that audit log entries are correctly recorded and persisted.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void record_shouldSaveAuditLogEntry() {
        auditLogService.record(new AuditLogService.AuditEvent(
                "CREATE", "Order", "123",
                "admin", "192.168.1.1",
                "method=OrderService.createOrder"
        ));

        ArgumentCaptor<AuditLog> captor =
            ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getEntityType()).isEqualTo("Order");
        assertThat(saved.getEntityId()).isEqualTo("123");
        assertThat(saved.getUserId()).isEqualTo("admin");
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(saved.getDetails())
            .isEqualTo("method=OrderService.createOrder");
        assertThat(saved.getEventTimestamp()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void record_shouldHandleNullOptionalFields() {
        auditLogService.record(new AuditLogService.AuditEvent(
                "LOGIN", "User", null,
                null, null, "method=AuthController.login"
        ));

        ArgumentCaptor<AuditLog> captor =
            ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo("LOGIN");
        assertThat(saved.getEntityType()).isEqualTo("User");
        assertThat(saved.getEntityId()).isNull();
        assertThat(saved.getUserId()).isNull();
        assertThat(saved.getIpAddress()).isNull();
    }
}
