-- V7: Create immutable audit log table for centralized audit trail.
-- Records all critical events: login, entity changes, API access, etc.
-- This table is append-only by convention (no UPDATE/DELETE grants).
CREATE TABLE audit_log (
    id              UUID         NOT NULL,
    event_timestamp TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id         VARCHAR(100),
    action          VARCHAR(50)  NOT NULL,
    entity_type     VARCHAR(50)  NOT NULL,
    entity_id       VARCHAR(100),
    ip_address      VARCHAR(45),
    details         TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_audit_log PRIMARY KEY (id)
);

-- Indexes for common query patterns
CREATE INDEX idx_audit_log_event_timestamp ON audit_log (event_timestamp);
CREATE INDEX idx_audit_log_user_id ON audit_log (user_id);
CREATE INDEX idx_audit_log_entity_type ON audit_log (entity_type);
CREATE INDEX idx_audit_log_action ON audit_log (action);
