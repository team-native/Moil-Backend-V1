CREATE TABLE images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    image_key VARCHAR(64) NOT NULL,
    image LONGBLOB NOT NULL,
    user_id BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_images_image_key UNIQUE (image_key),
    CONSTRAINT uk_images_user_id UNIQUE (user_id)
);

ALTER TABLE group_members
    ADD COLUMN image_path VARCHAR(255) NULL,
    MODIFY COLUMN color VARCHAR(20) NULL;
