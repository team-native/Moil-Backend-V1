CREATE TABLE event_availabilities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    available_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_event_availabilities_event_user_date
        UNIQUE (event_id, user_id, available_date),
    INDEX idx_event_availabilities_event_date (event_id, available_date),
    INDEX idx_event_availabilities_user_id (user_id)
);

CREATE TABLE event_availability_slots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    availability_id BIGINT NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_event_availability_slots_availability_id (availability_id)
);
