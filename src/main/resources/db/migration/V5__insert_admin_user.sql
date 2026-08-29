-- V5: Seed default admin user
-- Inserts a ADMIN account so the system is usable on first run.
-- password: ADMIN_PASSWORD (look at github actions secrets environment for the actual value)
-- BCrypt hash of the above password (generated with strength 12).
INSERT INTO
    users (
        id,
        username,
        email,
        password,
        role,
        is_account_non_expired,
        is_account_non_locked,
        is_credentials_non_expired,
        is_enabled,
        created_at,
        created_by,
        updated_at,
        updated_by
    )
VALUES (
        '00000000-0000-0000-0000-000000000001',
        'admin',
        'admin@oms.local',
        '$2a$12$SFADp0ZtNN7e9cuy/tnr8O4sfneaL/nBAHa.2RvjlXUT70MHlba4O',
        'ADMIN',
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        CURRENT_TIMESTAMP,
        'system',
        CURRENT_TIMESTAMP,
        'system'
    );