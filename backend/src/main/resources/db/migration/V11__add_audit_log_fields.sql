ALTER TABLE audit_logs ADD COLUMN previous_value TEXT;
ALTER TABLE audit_logs ADD COLUMN new_value TEXT;

ALTER TABLE loading_activities ADD COLUMN trailer_number VARCHAR(50);
