CREATE TABLE IF NOT EXISTS order_reviews (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    restaurant_id UUID NOT NULL,
    customer_user_id UUID NOT NULL,
    stars INTEGER NOT NULL,
    comment TEXT,
    merchant_response TEXT,
    responded_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT order_reviews_order_fk FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT order_reviews_restaurant_fk FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT order_reviews_customer_fk FOREIGN KEY (customer_user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT order_reviews_stars_ck CHECK (stars >= 1 AND stars <= 5)
);

CREATE UNIQUE INDEX IF NOT EXISTS order_reviews_order_uq ON order_reviews(order_id);
CREATE INDEX IF NOT EXISTS order_reviews_restaurant_created_idx ON order_reviews(restaurant_id, created_at DESC);
