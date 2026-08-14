ALTER TABLE images
    DROP INDEX uk_images_user_id;

CREATE INDEX idx_images_user_id ON images (user_id);
