CREATE TABLE user_device_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_device_tokens_token UNIQUE (token)
);

CREATE INDEX idx_user_device_tokens_user_id ON user_device_tokens (user_id);
