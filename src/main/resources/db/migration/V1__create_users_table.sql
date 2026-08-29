-- V1: Create users table
-- Stores application users with Spring Security account state flags.
CREATE TABLE users (
    id                        UUID PRIMARY KEY,
    username                  VARCHAR(50)  NOT NULL,
    email                     VARCHAR(100) NOT NULL,
    password                  VARCHAR(255) NOT NULL,
    role                      VARCHAR(20)  NOT NULL,
    is_account_non_expired    BOOLEAN      NOT NULL DEFAULT TRUE,
    is_account_non_locked     BOOLEAN      NOT NULL DEFAULT TRUE,
    is_credentials_non_expired BOOLEAN     NOT NULL DEFAULT TRUE,
    is_enabled                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                TIMESTAMP    NOT NULL,
    created_by                VARCHAR(100) NOT NULL,
    updated_at                TIMESTAMP,
    updated_by                VARCHAR(100),
    deleted_at                TIMESTAMP,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);