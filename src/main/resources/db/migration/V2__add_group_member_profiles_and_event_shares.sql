ALTER TABLE group_members
    ADD COLUMN nickname VARCHAR(10) NULL,
    ADD COLUMN color VARCHAR(20) NULL;

UPDATE group_members gm
JOIN user_accounts ua ON ua.id = gm.user_id
SET gm.nickname = LEFT(ua.name, 10),
    gm.color = 'RED'
WHERE gm.nickname IS NULL
   OR gm.color IS NULL;

ALTER TABLE group_members
    MODIFY COLUMN nickname VARCHAR(10) NOT NULL,
    MODIFY COLUMN color VARCHAR(20) NOT NULL;

ALTER TABLE events
    ADD COLUMN location VARCHAR(255) NULL;

UPDATE events
SET location = memo
WHERE location IS NULL
  AND memo IS NOT NULL;

CREATE TABLE event_shared_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_event_shared_members_event_user UNIQUE (event_id, user_id),
    INDEX idx_event_shared_members_event_id (event_id),
    INDEX idx_event_shared_members_user_id (user_id)
);

INSERT INTO event_shared_members (event_id, user_id, created_at)
SELECT id, creator_id, COALESCE(created_at, CURRENT_TIMESTAMP(6))
FROM events;
