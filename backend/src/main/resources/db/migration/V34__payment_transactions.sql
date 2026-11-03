-- Phase 8: VNPAY payment gateway support.
-- Widens the order payment-method constraint and adds the gateway transaction lifecycle table.
-- H2 test database has no CHECK constraints on these columns (Hibernate-validated only), but the
-- ALTERs are written to be safe on both PostgreSQL and H2.

ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_payment_method_ck;
ALTER TABLE orders ADD CONSTRAINT orders_payment_method_ck
    CHECK (payment_method IN ('COD', 'VNPAY'));

ALTER TABLE order_payments DROP CONSTRAINT IF EXISTS order_payments_method_ck;
ALTER TABLE order_payments ADD CONSTRAINT order_payments_method_ck
    CHECK (payment_method IN ('COD', 'VNPAY'));

CREATE TABLE IF NOT EXISTS payment_transactions (
    id                UUID PRIMARY KEY,
    order_id          UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    payment_method    VARCHAR(30) NOT NULL,
    gateway           VARCHAR(20) NOT NULL,
    gateway_txn_ref   VARCHAR(100),
    gateway_txn_no    VARCHAR(100),
    amount            BIGINT NOT NULL,
    currency          VARCHAR(3) NOT NULL DEFAULT 'VND',
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    gateway_response  TEXT,
    return_url        TEXT,
    payment_url       TEXT,
    ip_address        VARCHAR(45),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_payment_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED', 'CANCELLED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS payment_transactions_txn_ref_uq ON payment_transactions (gateway_txn_ref);
CREATE INDEX IF NOT EXISTS idx_payment_txn_order ON payment_transactions (order_id);
