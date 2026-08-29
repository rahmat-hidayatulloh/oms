-- V2: Create products table
-- Stores catalog products with optimistic locking via the version column.
CREATE TABLE products (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price       NUMERIC(10, 2) NOT NULL,
    stock       INTEGER       NOT NULL,
    category    VARCHAR(30)   NOT NULL,
    version     INTEGER       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP     NOT NULL,
    created_by  VARCHAR(100)  NOT NULL,
    updated_at  TIMESTAMP,
    updated_by  VARCHAR(100),
    deleted_at  TIMESTAMP
);
