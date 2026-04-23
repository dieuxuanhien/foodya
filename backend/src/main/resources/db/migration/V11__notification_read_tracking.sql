ALTER TABLE notification_logs
    ADD COLUMN IF NOT EXISTS read_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS notification_logs_receiver_read_idx
    ON notification_logs(receiver_user_id, read_at);
