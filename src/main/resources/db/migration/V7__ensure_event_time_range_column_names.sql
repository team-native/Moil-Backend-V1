SET @rename_event_start_column = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'events'
              AND column_name = 'starts_at'
        )
        AND NOT EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'events'
              AND column_name = 'start_date'
        ),
        'ALTER TABLE events RENAME COLUMN starts_at TO start_date',
        'SELECT 1'
    )
);

PREPARE rename_event_start_column_statement FROM @rename_event_start_column;
EXECUTE rename_event_start_column_statement;
DEALLOCATE PREPARE rename_event_start_column_statement;

SET @rename_event_end_column = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'events'
              AND column_name = 'ends_at'
        )
        AND NOT EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'events'
              AND column_name = 'end_date'
        ),
        'ALTER TABLE events RENAME COLUMN ends_at TO end_date',
        'SELECT 1'
    )
);

PREPARE rename_event_end_column_statement FROM @rename_event_end_column;
EXECUTE rename_event_end_column_statement;
DEALLOCATE PREPARE rename_event_end_column_statement;
