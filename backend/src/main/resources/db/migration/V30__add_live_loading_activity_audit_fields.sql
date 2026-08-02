ALTER TABLE loading_activities
    ADD COLUMN IF NOT EXISTS started_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS completed_by_name VARCHAR(100);
