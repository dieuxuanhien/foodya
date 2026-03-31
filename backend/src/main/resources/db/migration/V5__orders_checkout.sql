CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    order_code VARCHAR(30) NOT NULL,
    customer_user_id UUID NOT NULL,
    idempotency_key VARCHAR(80) NOT NULL,
    restaurant_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    delivery_address TEXT NOT NULL,
    delivery_latitude NUMERIC(10,7) NOT NULL,
    delivery_longitude NUMERIC(10,7) NOT NULL,
    customer_note TEXT,
    subtotal_amount NUMERIC(12,2) NOT NULL,
    delivery_fee NUMERIC(12,2) NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    payment_method VARCHAR(16) NOT NULL,
    payment_status VARCHAR(16) NOT NULL,
    commission_amount NUMERIC(12,2) NOT NULL,
    shipping_fee_margin_amount NUMERIC(12,2) NOT NULL,
    platform_profit_amount NUMERIC(12,2) NOT NULL,
    cancel_reason VARCHAR(255),
    placed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT orders_customer_fk FOREIGN KEY (customer_user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT orders_restaurant_fk FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT orders_status_ck CHECK (status IN ('PENDING', 'ACCEPTED', 'ASSIGNED', 'PREPARING', 'DELIVERING', 'SUCCESS', 'CANCELLED', 'FAILED')),
    CONSTRAINT orders_payment_method_ck CHECK (payment_method IN ('COD')),
    CONSTRAINT orders_payment_status_ck CHECK (payment_status IN ('UNPAID', 'PAID', 'FAILED', 'REFUNDED')),
    CONSTRAINT orders_amount_non_negative_ck CHECK (subtotal_amount >= 0 AND delivery_fee >= 0 AND total_amount >= 0),
    CONSTRAINT orders_total_consistency_ck CHECK (total_amount = subtotal_amount + delivery_fee)
);

CREATE UNIQUE INDEX IF NOT EXISTS orders_code_uq ON orders(order_code);
CREATE UNIQUE INDEX IF NOT EXISTS orders_idempotency_uq ON orders(customer_user_id, idempotency_key);
CREATE INDEX IF NOT EXISTS orders_customer_created_idx ON orders(customer_user_id, placed_at DESC);
CREATE INDEX IF NOT EXISTS orders_restaurant_status_idx ON orders(restaurant_id, status, placed_at DESC);

CREATE TABLE IF NOT EXISTS order_payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    payment_method VARCHAR(16) NOT NULL,
    payment_status VARCHAR(16) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE,
    external_ref VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT order_payments_order_fk FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT order_payments_method_ck CHECK (payment_method IN ('COD')),
    CONSTRAINT order_payments_status_ck CHECK (payment_status IN ('UNPAID', 'PAID', 'FAILED', 'REFUNDED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS order_payments_order_uq ON order_payments(order_id);

CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    menu_item_id UUID NOT NULL,
    menu_item_name_snapshot VARCHAR(180) NOT NULL,
    unit_price_snapshot NUMERIC(12,2) NOT NULL,
    quantity INTEGER NOT NULL,
    line_total NUMERIC(12,2) NOT NULL,
    CONSTRAINT order_items_order_fk FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT order_items_menu_item_fk FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT order_items_qty_ck CHECK (quantity > 0),
    CONSTRAINT order_items_line_total_ck CHECK (line_total >= 0)
);

CREATE INDEX IF NOT EXISTS order_items_order_idx ON order_items(order_id);
