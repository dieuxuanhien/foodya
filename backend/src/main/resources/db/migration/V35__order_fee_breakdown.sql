-- Phase 10: persist the delivery-fee breakdown used at order creation (audit + customer transparency).
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fee_breakdown TEXT;
