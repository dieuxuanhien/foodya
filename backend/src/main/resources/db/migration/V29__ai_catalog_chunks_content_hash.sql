ALTER TABLE ai_catalog_chunks ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64) NOT NULL DEFAULT '';

ALTER TABLE ai_catalog_chunks ADD CONSTRAINT ai_catalog_chunks_menu_item_unique UNIQUE (menu_item_id);
