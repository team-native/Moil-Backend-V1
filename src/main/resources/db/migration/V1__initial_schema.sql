CREATE TABLE user_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_accounts_email UNIQUE (email)
);

CREATE TABLE email_verifications (
    verify_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100) NULL,
    step VARCHAR(20) NULL,
    code VARCHAR(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (verify_id)
);

CREATE TABLE verified_signup_sessions (
    session_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100) NULL,
    step VARCHAR(20) NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (session_id)
);

CREATE TABLE login_sessions (
    session_id VARCHAR(255) NOT NULL,
    access_token VARCHAR(1024) NOT NULL,
    user_id BIGINT NOT NULL,
    refresh_token VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (session_id)
);

CREATE TABLE user_groups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    invite_code VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_groups_invite_code UNIQUE (invite_code)
);

CREATE TABLE group_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    owner_group_id BIGINT NULL,
    role VARCHAR(20) NOT NULL,
    notification_enabled BIT NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_group_members_group_user UNIQUE (group_id, user_id),
    CONSTRAINT uk_group_members_owner_group UNIQUE (owner_group_id)
);

CREATE TABLE events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    creator_id BIGINT NOT NULL,
    updater_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    memo VARCHAR(1000) NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_group_members_user_id ON group_members (user_id);
CREATE INDEX idx_group_members_group_id ON group_members (group_id);
CREATE INDEX idx_events_group_id ON events (group_id);
CREATE INDEX idx_events_creator_id ON events (creator_id);
CREATE INDEX idx_login_sessions_access_token ON login_sessions (access_token(255));
