CREATE TABLE IF NOT EXISTS category_taxonomies (
    code VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO category_taxonomies (code, display_name, sort_order, is_active, created_at, updated_at)
SELECT v.code, v.display_name, v.sort_order, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    VALUES
        ('APPETIZERS', 'Appetizers', 10),
        ('BREAKFAST', 'Breakfast', 20),
        ('BOWLS', 'Bowls', 30),
        ('DESSERTS', 'Desserts', 40),
        ('DRINKS', 'Drinks', 50),
        ('FAST_FOOD', 'Fast Food', 60),
        ('FUSION', 'Fusion', 70),
        ('MAIN_DISHES', 'Main Dishes', 80),
        ('NOODLES', 'Noodles', 90),
        ('RICE', 'Rice', 100),
        ('SALADS', 'Salads', 110),
        ('SEAFOOD', 'Seafood', 120),
        ('SNACKS', 'Snacks', 130),
        ('SOUPS', 'Soups', 140),
        ('VEGETARIAN', 'Vegetarian', 150),
        ('VEGAN', 'Vegan', 160)
) AS v(code, display_name, sort_order)
WHERE NOT EXISTS (
    SELECT 1
    FROM category_taxonomies ct
    WHERE ct.code = v.code
);

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
