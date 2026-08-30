CREATE TABLE transport_distance_rates (
    id BIGSERIAL PRIMARY KEY,
    minimum_km NUMERIC(10,3) NOT NULL,
    maximum_km NUMERIC(10,3) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_transport_distance_rate_bounds CHECK (minimum_km >= 0 AND maximum_km >= minimum_km),
    CONSTRAINT chk_transport_distance_rate_price CHECK (price >= 0)
);

ALTER TABLE company_settings
    ADD COLUMN depot_name VARCHAR(150),
    ADD COLUMN depot_address VARCHAR(255),
    ADD COLUMN depot_latitude NUMERIC(10,7),
    ADD COLUMN depot_longitude NUMERIC(10,7);

ALTER TABLE fuel_orders
    ADD COLUMN delivery_address TEXT,
    ADD COLUMN delivery_latitude NUMERIC(10,7),
    ADD COLUMN delivery_longitude NUMERIC(10,7),
    ADD COLUMN delivery_distance_km NUMERIC(10,3),
    ADD COLUMN distance_transport_price NUMERIC(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN route_duration_seconds BIGINT,
    ADD COLUMN route_polyline TEXT,
    ADD COLUMN route_provider VARCHAR(50),
    ADD COLUMN route_type VARCHAR(50);
