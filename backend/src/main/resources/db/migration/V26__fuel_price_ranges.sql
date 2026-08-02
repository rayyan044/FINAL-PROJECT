CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE fuel_price_ranges (
    id BIGSERIAL PRIMARY KEY,
    fuel_product_id BIGINT NOT NULL REFERENCES fuel_products(id),
    min_litres DECIMAL(12,2) NOT NULL,
    max_litres DECIMAL(12,2) NOT NULL,
    price_per_litre DECIMAL(12,2) NOT NULL,
    effective_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_fuel_price_range_litres CHECK (min_litres >= 0.01 AND max_litres >= min_litres),
    CONSTRAINT chk_fuel_price_range_price CHECK (price_per_litre >= 0),
    CONSTRAINT chk_fuel_price_range_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

ALTER TABLE fuel_price_ranges
    ADD CONSTRAINT fuel_price_ranges_no_overlap
    EXCLUDE USING gist (
        fuel_product_id WITH =,
        numrange(min_litres, max_litres, '[]') WITH &&
    ) WHERE (deleted = FALSE);

CREATE INDEX idx_fuel_price_ranges_lookup
    ON fuel_price_ranges (fuel_product_id, status, effective_date DESC, min_litres, max_litres)
    WHERE deleted = FALSE;

ALTER TABLE fuel_orders ADD COLUMN unit_price DECIMAL(12,2);
