-- Phase 2: Driver Lifecycle System (Grab-Style Toggle)
-- Creates the driver_online_sessions table for tracking driver online/offline state machine.

CREATE TABLE driver_online_sessions (
    id                   UUID PRIMARY KEY,
    driver_user_id       UUID NOT NULL REFERENCES users(id),
    status               VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    -- ONLINE:         accepting orders (driver toggled ON)
    -- ON_DELIVERY:    currently delivering one or more orders
    -- BUSY:           at max concurrent orders
    -- GOING_OFFLINE:  "last trip" mode — finish current, then auto-offline
    -- OFFLINE:        toggled off (ended_at is set)
    -- SUSPENDED:      admin-blocked
    started_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at             TIMESTAMP WITH TIME ZONE,            -- filled when driver toggles OFF or auto-offline
    last_heartbeat       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    h3_index_res9        VARCHAR(64),             -- current H3 cell (updated with location)
    current_lat          DOUBLE PRECISION,
    current_lng          DOUBLE PRECISION,
    active_order_count   INTEGER NOT NULL DEFAULT 0,
    max_concurrent_orders INTEGER NOT NULL DEFAULT 2,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_driver_session_status
        CHECK (status IN ('ONLINE','ON_DELIVERY','BUSY','GOING_OFFLINE','OFFLINE','SUSPENDED'))
);

-- Fast lookup for a driver's active session
CREATE INDEX idx_driver_sessions_active
    ON driver_online_sessions (driver_user_id, status);

-- Fast spatial lookup: drivers in a given H3 cell
CREATE INDEX idx_driver_sessions_h3
    ON driver_online_sessions (h3_index_res9);
