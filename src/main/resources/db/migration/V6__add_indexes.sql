-- V6: Add database indexes for frequently queried columns.
-- Improves query performance for product search by category, order queries by
-- user/status, and order-items lookups by product (e.g. stock reporting, analytics).
CREATE INDEX idx_products_category ON products (category);
CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);