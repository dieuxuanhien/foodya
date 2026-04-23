CREATE TABLE IF NOT EXISTS notification_logs (
    id UUID PRIMARY KEY,
    receiver_user_id UUID NOT NULL,
    receiver_type VARCHAR(32) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    order_id UUID,
    provider_response TEXT,
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT notification_logs_receiver_fk FOREIGN KEY (receiver_user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT notification_logs_order_fk FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT notification_logs_status_ck CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'SKIPPED'))
);

CREATE INDEX IF NOT EXISTS notification_logs_receiver_created_idx
    ON notification_logs(receiver_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS notification_logs_created_idx
    ON notification_logs(created_at DESC);
