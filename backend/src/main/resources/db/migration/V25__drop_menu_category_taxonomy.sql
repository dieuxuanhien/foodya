-- Taxonomy ownership is moved to menu items via menu_item_taxonomies.
-- Keep a safe default taxonomy for any menu item that still has no mapping.
INSERT INTO menu_item_taxonomies (menu_item_id, taxonomy_code)
SELECT mi.id, 'MAIN_DISHES'
FROM menu_items mi
LEFT JOIN menu_item_taxonomies mit ON mit.menu_item_id = mi.id
WHERE mit.menu_item_id IS NULL
ON CONFLICT DO NOTHING;

ALTER TABLE menu_categories
    DROP CONSTRAINT IF EXISTS menu_categories_taxonomy_fk;

DROP INDEX IF EXISTS menu_categories_taxonomy_idx;

ALTER TABLE menu_categories
    DROP COLUMN IF EXISTS taxonomy_code;
