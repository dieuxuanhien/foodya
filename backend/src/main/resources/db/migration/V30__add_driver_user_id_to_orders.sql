-- V30: Add driver_user_id to orders table for driver assignment tracking.
-- Previously, the system relied on a manual delivery accept flow without persisting
-- which driver is assigned. This column enables auto-assignment and lookup.

ALTER TABLE orders ADD COLUMN driver_user_id UUID;

ALTER TABLE orders ADD CONSTRAINT orders_driver_fk
    FOREIGN KEY (driver_user_id) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE;

CREATE INDEX IF NOT EXISTS idx_orders_driver_user_id ON orders(driver_user_id);
