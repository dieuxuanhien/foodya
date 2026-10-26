-- Phase 6: stored delivery routes (polyline + waypoints) per order.
-- Written to run on PostgreSQL and H2 (see V32 notes): app-assigned UUIDs, TEXT JSON, no partial indexes.
CREATE TABLE IF NOT EXISTS delivery_routes (
    id               UUID PRIMARY KEY,
    order_id         UUID NOT NULL REFERENCES orders(id),
    batch_id         UUID REFERENCES order_batches(id),
    encoded_polyline TEXT NOT NULL,
    distance_meters  INTEGER NOT NULL,
    duration_seconds INTEGER NOT NULL,
    waypoints        TEXT,
    calculated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_delivery_routes_order ON delivery_routes (order_id, is_active);
CREATE INDEX IF NOT EXISTS idx_delivery_routes_batch ON delivery_routes (batch_id, is_active);
