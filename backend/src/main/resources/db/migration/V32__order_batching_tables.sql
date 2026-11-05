-- Phase 5: Order Batching Tables
-- Supports VRPPD-TW batch assignment of orders to drivers.
--
-- SQL is written to run on both PostgreSQL and H2 (MODE=PostgreSQL) since the
-- integration test suite boots the schema on H2 and Hibernate then validates it:
--   * UUID PKs are assigned by the application (@PrePersist), not by a DB default.
--   * JSON payloads are stored as TEXT (H2 has no JSONB type).
--   * Timestamps use TIMESTAMP WITH TIME ZONE (not the TIMESTAMPTZ shorthand).
--   * No partial indexes (H2 rejects a WHERE clause on CREATE INDEX).

-- 1. order_batches: groups multiple orders assigned to a single driver
CREATE TABLE IF NOT EXISTS order_batches (
    id                       UUID PRIMARY KEY,
    driver_user_id           UUID NOT NULL REFERENCES users(id),
    session_id               UUID NOT NULL REFERENCES driver_online_sessions(id),
    status                   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    pickup_sequence          TEXT NOT NULL,
    delivery_sequence        TEXT NOT NULL,
    total_distance_meters    INTEGER,
    total_duration_seconds   INTEGER,
    optimized_route_polyline TEXT,
    solver_metadata          TEXT,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at               TIMESTAMP WITH TIME ZONE,
    completed_at             TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_batch_status CHECK (
        status IN ('PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'PARTIAL')
    )
);

CREATE INDEX IF NOT EXISTS idx_batch_driver_active
    ON order_batches (driver_user_id, status);

CREATE INDEX IF NOT EXISTS idx_batch_created
    ON order_batches (created_at);

-- 2. batch_order_items: individual orders within a batch
CREATE TABLE IF NOT EXISTS batch_order_items (
    id                      UUID PRIMARY KEY,
    batch_id                UUID NOT NULL REFERENCES order_batches(id) ON DELETE CASCADE,
    order_id                UUID NOT NULL REFERENCES orders(id),
    pickup_index            INTEGER NOT NULL,
    delivery_index          INTEGER NOT NULL,
    estimated_pickup_at     TIMESTAMP WITH TIME ZONE,
    estimated_delivery_at   TIMESTAMP WITH TIME ZONE,
    actual_pickup_at        TIMESTAMP WITH TIME ZONE,
    actual_delivery_at      TIMESTAMP WITH TIME ZONE,
    item_status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_batch_order UNIQUE (batch_id, order_id),
    CONSTRAINT chk_item_status CHECK (
        item_status IN ('PENDING', 'PICKED_UP', 'DELIVERING', 'DELIVERED', 'FAILED')
    )
);

CREATE INDEX IF NOT EXISTS idx_batch_items_batch ON batch_order_items (batch_id);
CREATE INDEX IF NOT EXISTS idx_batch_items_order ON batch_order_items (order_id);

-- 3. Alter orders table for batch support
ALTER TABLE orders ADD COLUMN IF NOT EXISTS batch_id UUID;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS batch_pickup_index INTEGER;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS batch_delivery_index INTEGER;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS estimated_delivery_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_orders_batch ON orders (batch_id);
CREATE INDEX IF NOT EXISTS idx_orders_accepted_unbatched ON orders (status, placed_at);

-- 4. distance_matrix_cache: caches Goong distance matrix results
--    (currently a future hook — the adapter performs live calls; kept for later use)
CREATE TABLE IF NOT EXISTS distance_matrix_cache (
    id               UUID PRIMARY KEY,
    origin_h3_res8   VARCHAR(20) NOT NULL,
    dest_h3_res8     VARCHAR(20) NOT NULL,
    distance_meters  INTEGER NOT NULL,
    duration_seconds INTEGER NOT NULL,
    vehicle_type     VARCHAR(10) NOT NULL DEFAULT 'bike',
    fetched_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_dm_cache UNIQUE (origin_h3_res8, dest_h3_res8, vehicle_type)
);

CREATE INDEX IF NOT EXISTS idx_dm_cache_lookup
    ON distance_matrix_cache (origin_h3_res8, dest_h3_res8, vehicle_type);
