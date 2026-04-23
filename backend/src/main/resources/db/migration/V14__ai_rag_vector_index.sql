CREATE TABLE IF NOT EXISTS ai_catalog_chunks (
    id UUID PRIMARY KEY,
    menu_item_id UUID NOT NULL,
    restaurant_id UUID NOT NULL,
    chunk_text TEXT NOT NULL,
    chunk_metadata TEXT NOT NULL,
    embedding_text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ai_catalog_chunks_menu_item_fk FOREIGN KEY (menu_item_id)
        REFERENCES menu_items(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ai_catalog_chunks_restaurant_fk FOREIGN KEY (restaurant_id)
        REFERENCES restaurants(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS ai_catalog_chunks_menu_idx ON ai_catalog_chunks(menu_item_id);
CREATE INDEX IF NOT EXISTS ai_catalog_chunks_restaurant_idx ON ai_catalog_chunks(restaurant_id);
