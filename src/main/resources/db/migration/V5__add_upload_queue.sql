CREATE TABLE upload_queue (
    user_id BIGINT NOT NULL,
    pending_key VARCHAR(64) NOT NULL,
    image LONGBLOB NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_upload_queue_pending_key UNIQUE (pending_key)
);
