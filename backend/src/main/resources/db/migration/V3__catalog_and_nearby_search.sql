CREATE TABLE IF NOT EXISTS restaurants (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    name VARCHAR(180) NOT NULL,
    cuisine_type VARCHAR(80) NOT NULL,
    description TEXT,
    address_line TEXT NOT NULL,
    latitude NUMERIC(10,7) NOT NULL,
    longitude NUMERIC(10,7) NOT NULL,
    h3_index_res9 VARCHAR(32) NOT NULL,
    avg_rating NUMERIC(3,2) NOT NULL DEFAULT 0,
    review_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    is_open BOOLEAN NOT NULL DEFAULT FALSE,
    max_delivery_km NUMERIC(8,3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT restaurants_owner_fk FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS restaurants_owner_idx ON restaurants(owner_user_id);
CREATE INDEX IF NOT EXISTS restaurants_h3_idx ON restaurants(h3_index_res9);
CREATE INDEX IF NOT EXISTS restaurants_name_search_idx ON restaurants(name);

CREATE TABLE IF NOT EXISTS menu_categories (
    id UUID PRIMARY KEY,
    restaurant_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT menu_categories_restaurant_fk FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT menu_categories_rest_name_uq UNIQUE (restaurant_id, name)
);

CREATE INDEX IF NOT EXISTS menu_categories_restaurant_idx ON menu_categories(restaurant_id, is_active, sort_order, name);

CREATE TABLE IF NOT EXISTS menu_items (
    id UUID PRIMARY KEY,
    restaurant_id UUID NOT NULL,
    category_id UUID NOT NULL,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    price NUMERIC(12,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT menu_items_restaurant_fk FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT menu_items_category_fk FOREIGN KEY (category_id) REFERENCES menu_categories(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS menu_items_restaurant_active_idx ON menu_items(restaurant_id, is_active, is_available);
CREATE INDEX IF NOT EXISTS menu_items_restaurant_category_idx ON menu_items(restaurant_id, category_id);
CREATE INDEX IF NOT EXISTS menu_items_name_search_idx ON menu_items(name);
