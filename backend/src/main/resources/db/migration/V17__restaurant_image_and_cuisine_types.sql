ALTER TABLE restaurants
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(1024);

ALTER TABLE restaurants
    ADD COLUMN IF NOT EXISTS cuisine_types TEXT;

UPDATE restaurants
SET cuisine_types = cuisine_type
WHERE cuisine_types IS NULL OR TRIM(cuisine_types) = '';
