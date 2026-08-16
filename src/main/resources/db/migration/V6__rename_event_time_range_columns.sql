ALTER TABLE events
    RENAME COLUMN starts_at TO start_date,
    RENAME COLUMN ends_at TO end_date;
