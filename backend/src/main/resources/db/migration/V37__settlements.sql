-- Settlements: the admin marks a payee's outstanding balance as paid (or collected). No money moves in
-- the system; these tables are the audit trail. Lines reference orders without a foreign key so the
-- ledger survives an order hard-delete, and the unique constraint stops one order being settled twice.

CREATE TABLE IF NOT EXISTS settlements (
    id                    UUID PRIMARY KEY,
    payee_type            VARCHAR(16) NOT NULL,
    payee_user_id         UUID NOT NULL,
    direction             VARCHAR(16) NOT NULL,
    order_count           INTEGER NOT NULL,
    gross_amount          NUMERIC(14, 2) NOT NULL,
    platform_fee_amount   NUMERIC(14, 2) NOT NULL,
    cash_collected_amount NUMERIC(14, 2) NOT NULL,
    net_amount            NUMERIC(14, 2) NOT NULL,
    note                  TEXT,
    settled_by            VARCHAR(128) NOT NULL,
    settled_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT settlements_payee_type_ck CHECK (payee_type IN ('MERCHANT', 'DRIVER')),
    CONSTRAINT settlements_direction_ck CHECK (direction IN ('PAYOUT', 'COLLECTION'))
);

CREATE INDEX IF NOT EXISTS idx_settlements_payee ON settlements (payee_type, payee_user_id, settled_at);
CREATE INDEX IF NOT EXISTS idx_settlements_settled_at ON settlements (settled_at);

CREATE TABLE IF NOT EXISTS settlement_lines (
    id                    UUID PRIMARY KEY,
    settlement_id         UUID NOT NULL REFERENCES settlements (id) ON DELETE CASCADE,
    order_id              UUID NOT NULL,
    order_code            VARCHAR(30) NOT NULL,
    payee_type            VARCHAR(16) NOT NULL,
    gross_amount          NUMERIC(14, 2) NOT NULL,
    platform_fee_amount   NUMERIC(14, 2) NOT NULL,
    cash_collected_amount NUMERIC(14, 2) NOT NULL,
    net_amount            NUMERIC(14, 2) NOT NULL,
    CONSTRAINT settlement_lines_order_payee_uq UNIQUE (order_id, payee_type)
);

CREATE INDEX IF NOT EXISTS idx_settlement_lines_settlement ON settlement_lines (settlement_id);

-- Platform commission on the food subtotal: 15% (was 10%). Only moves rows still on the old default, so an
-- admin who already tuned the rate keeps their value.
UPDATE system_parameters
SET param_value = '15.0',
    version = version + 1,
    updated_by_actor = 'SYSTEM',
    updated_at = CURRENT_TIMESTAMP
WHERE param_key = 'finance.commission_rate_percent'
  AND param_value = '10.0';