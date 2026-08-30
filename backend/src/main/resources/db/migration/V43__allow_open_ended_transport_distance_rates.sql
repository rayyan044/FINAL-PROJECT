ALTER TABLE transport_distance_rates
    ALTER COLUMN maximum_km DROP NOT NULL;

ALTER TABLE transport_distance_rates
    DROP CONSTRAINT chk_transport_distance_rate_bounds;

ALTER TABLE transport_distance_rates
    ADD CONSTRAINT chk_transport_distance_rate_bounds
        CHECK (minimum_km >= 0 AND (maximum_km IS NULL OR maximum_km >= minimum_km));
