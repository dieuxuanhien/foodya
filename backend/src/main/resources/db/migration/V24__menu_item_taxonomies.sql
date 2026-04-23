CREATE TABLE IF NOT EXISTS menu_item_taxonomies (
    menu_item_id UUID NOT NULL,
    taxonomy_code VARCHAR(64) NOT NULL,
    PRIMARY KEY (menu_item_id, taxonomy_code),
    CONSTRAINT menu_item_taxonomies_menu_item_fk
        FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
        ON DELETE CASCADE,
    CONSTRAINT menu_item_taxonomies_taxonomy_fk
        FOREIGN KEY (taxonomy_code) REFERENCES category_taxonomies(code)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS menu_item_taxonomies_taxonomy_idx
    ON menu_item_taxonomies(taxonomy_code, menu_item_id);

INSERT INTO menu_item_taxonomies (menu_item_id, taxonomy_code)
SELECT mi.id, COALESCE(mc.taxonomy_code, 'MAIN_DISHES')
FROM menu_items mi
JOIN menu_categories mc ON mc.id = mi.category_id
ON CONFLICT DO NOTHING;
