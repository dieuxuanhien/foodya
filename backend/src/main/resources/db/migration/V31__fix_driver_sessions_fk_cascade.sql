-- V31: Fix driver_online_sessions FK to cascade delete when user is removed.
-- This fixes test cleanup failures where deleting test users is blocked by the FK.

-- H2 doesn't name inline constraints the same way, so we drop and recreate the table's FK.
-- For H2 compatibility, we alter the constraint by dropping the old one and adding a new named one.

ALTER TABLE driver_online_sessions DROP CONSTRAINT IF EXISTS CONSTRAINT_B9D;

-- Also try dropping by the FK column reference pattern (in case the name differs)
ALTER TABLE driver_online_sessions ADD CONSTRAINT fk_driver_sessions_user
    FOREIGN KEY (driver_user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE;
