-- Refund support: the VNPAY refund API needs the original transaction date, and the owning
-- gateway transaction number is stored back after a successful refund.

ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS transaction_date VARCHAR(14);
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS refund_txn_no VARCHAR(100);

-- Timeout sweeps read orders by status + placed_at and driver sessions by heartbeat age.
CREATE INDEX IF NOT EXISTS idx_orders_status_placed_at ON orders (status, placed_at);
CREATE INDEX IF NOT EXISTS idx_orders_driver_status ON orders (driver_user_id, status);
CREATE INDEX IF NOT EXISTS idx_driver_sessions_heartbeat ON driver_online_sessions (last_heartbeat);
CREATE INDEX IF NOT EXISTS idx_payment_txn_status_created ON payment_transactions (status, created_at);