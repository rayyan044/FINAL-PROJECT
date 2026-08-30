ALTER TABLE deliveries
    ADD COLUMN IF NOT EXISTS current_latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS current_longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS current_location_accuracy DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS location_updated_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_deliveries_live_location
    ON deliveries (delivery_status, location_updated_at DESC);
