CREATE TABLE social_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_social_accounts_provider_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT uk_social_accounts_provider_user UNIQUE (provider, user_id),
    CONSTRAINT fk_social_accounts_user FOREIGN KEY (user_id) REFERENCES user_accounts (id) ON DELETE CASCADE
);

CREATE INDEX idx_social_accounts_user_id ON social_accounts (user_id);
