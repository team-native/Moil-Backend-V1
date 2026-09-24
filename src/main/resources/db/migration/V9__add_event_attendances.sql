CREATE TABLE event_attendances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_event_attendances_event_user UNIQUE (event_id, user_id)
);

CREATE INDEX idx_event_attendances_event_id ON event_attendances (event_id);
CREATE INDEX idx_event_attendances_user_id ON event_attendances (user_id);
