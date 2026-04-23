ALTER TABLE restaurants
    ADD COLUMN IF NOT EXISTS background_image_url VARCHAR(1024);

ALTER TABLE restaurants
    ADD COLUMN IF NOT EXISTS avatar_image_url VARCHAR(1024);

UPDATE restaurants
SET background_image_url = COALESCE(background_image_url, image_url)
WHERE background_image_url IS NULL;