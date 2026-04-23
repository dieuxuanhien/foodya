CREATE TABLE IF NOT EXISTS carts (
    id UUID PRIMARY KEY,
    customer_user_id UUID NOT NULL,
    restaurant_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT carts_customer_fk FOREIGN KEY (customer_user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT carts_restaurant_fk FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT carts_status_ck CHECK (status IN ('ACTIVE', 'CHECKED_OUT', 'ABANDONED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS cart_active_customer_uq ON carts(customer_user_id, status);
CREATE INDEX IF NOT EXISTS carts_restaurant_idx ON carts(restaurant_id);

CREATE TABLE IF NOT EXISTS cart_items (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL,
    menu_item_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price_snapshot NUMERIC(12,2) NOT NULL,
    note VARCHAR(255),
    CONSTRAINT cart_items_cart_fk FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT cart_items_menu_item_fk FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT cart_items_qty_ck CHECK (quantity > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS cart_items_cart_menu_uq ON cart_items(cart_id, menu_item_id);
CREATE INDEX IF NOT EXISTS cart_items_cart_idx ON cart_items(cart_id);
