-- V3: Create orders table
-- Stores customer orders. user_id references users(id) but is not a hard FK
-- to keep the order history intact even after a user is soft-deleted.
CREATE TABLE orders (
    id           UUID PRIMARY KEY,
    order_number VARCHAR(50)  NOT NULL,
    user_id      UUID         NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMP    NOT NULL,
    created_by   VARCHAR(100) NOT NULL,
    updated_at   TIMESTAMP,
    updated_by   VARCHAR(100),
    deleted_at   TIMESTAMP,
    CONSTRAINT uk_orders_order_number UNIQUE (order_number)
);
