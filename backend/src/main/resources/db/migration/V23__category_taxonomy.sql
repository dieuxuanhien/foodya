CREATE TABLE IF NOT EXISTS category_taxonomies (
    code VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO category_taxonomies (code, display_name, sort_order, is_active, created_at, updated_at)
VALUES
    ('APPETIZERS', 'Appetizers', 10, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('BREAKFAST', 'Breakfast', 20, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('BOWLS', 'Bowls', 30, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DESSERTS', 'Desserts', 40, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DRINKS', 'Drinks', 50, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('FAST_FOOD', 'Fast Food', 60, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('FUSION', 'Fusion', 70, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MAIN_DISHES', 'Main Dishes', 80, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('NOODLES', 'Noodles', 90, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('RICE', 'Rice', 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SALADS', 'Salads', 110, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SEAFOOD', 'Seafood', 120, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SNACKS', 'Snacks', 130, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SOUPS', 'Soups', 140, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('VEGETARIAN', 'Vegetarian', 150, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('VEGAN', 'Vegan', 160, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

ALTER TABLE menu_categories
    ADD COLUMN IF NOT EXISTS taxonomy_code VARCHAR(64);

UPDATE menu_categories
SET taxonomy_code = CASE
    WHEN lower(name) LIKE '%appetizer%' THEN 'APPETIZERS'
    WHEN lower(name) LIKE '%starter%' THEN 'APPETIZERS'
    WHEN lower(name) LIKE '%salad%' THEN 'SALADS'
    WHEN lower(name) LIKE '%soup%' THEN 'SOUPS'
    WHEN lower(name) LIKE '%noodle%' THEN 'NOODLES'
    WHEN lower(name) LIKE '%rice%' THEN 'RICE'
    WHEN lower(name) LIKE '%dessert%' THEN 'DESSERTS'
    WHEN lower(name) LIKE '%drink%' OR lower(name) LIKE '%beverage%' THEN 'DRINKS'
    WHEN lower(name) LIKE '%seafood%' THEN 'SEAFOOD'
    WHEN lower(name) LIKE '%breakfast%' THEN 'BREAKFAST'
    WHEN lower(name) LIKE '%snack%' THEN 'SNACKS'
    WHEN lower(name) LIKE '%vegan%' THEN 'VEGAN'
    WHEN lower(name) LIKE '%vegetarian%' THEN 'VEGETARIAN'
    WHEN lower(name) LIKE '%burger%' OR lower(name) LIKE '%sandwich%' THEN 'FAST_FOOD'
    WHEN lower(name) LIKE '%main%' OR lower(name) LIKE '%mains%' THEN 'MAIN_DISHES'
    ELSE 'MAIN_DISHES'
END
WHERE taxonomy_code IS NULL OR taxonomy_code = '';

ALTER TABLE menu_categories
    ALTER COLUMN taxonomy_code SET NOT NULL;

ALTER TABLE menu_categories
    ADD CONSTRAINT menu_categories_taxonomy_fk
    FOREIGN KEY (taxonomy_code) REFERENCES category_taxonomies(code)
    ON UPDATE CASCADE
    ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS menu_categories_taxonomy_idx
    ON menu_categories(taxonomy_code, is_active, sort_order, name);
