-- V4: Create order_items table
-- Stores the line items of an order. order_id is a hard FK to orders(id)
-- with cascade delete so items are removed together with their parent order.
CREATE TABLE order_items (
    id           UUID PRIMARY KEY,
    product_id   UUID         NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    quantity     INTEGER      NOT NULL,
    price        NUMERIC(10, 2) NOT NULL,
    order_id     UUID         NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    created_by   VARCHAR(100) NOT NULL,
    updated_at   TIMESTAMP,
    updated_by   VARCHAR(100),
    deleted_at   TIMESTAMP,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON DELETE CASCADE
);
