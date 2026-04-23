CREATE TABLE IF NOT EXISTS order_tracking_points (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    lat NUMERIC(10,7) NOT NULL,
    lng NUMERIC(10,7) NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT otp_order_fk FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS otp_order_recorded_idx ON order_tracking_points(order_id, recorded_at ASC);
