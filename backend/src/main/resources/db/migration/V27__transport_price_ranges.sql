CREATE TABLE transport_price_ranges (
    id BIGSERIAL PRIMARY KEY,
    min_litres DECIMAL(12,2) NOT NULL,
    max_litres DECIMAL(12,2) NOT NULL,
    transport_price DECIMAL(12,2) NOT NULL,
    effective_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100), updated_by VARCHAR(100), deleted BOOLEAN NOT NULL DEFAULT FALSE, deleted_at TIMESTAMP,
    CONSTRAINT chk_transport_range_litres CHECK (min_litres >= 0.01 AND max_litres >= min_litres),
    CONSTRAINT chk_transport_range_price CHECK (transport_price >= 0),
    CONSTRAINT chk_transport_range_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
ALTER TABLE transport_price_ranges ADD CONSTRAINT transport_price_ranges_no_overlap
    EXCLUDE USING gist (numrange(min_litres, max_litres, '[]') WITH &&) WHERE (deleted = FALSE);
CREATE INDEX idx_transport_price_ranges_lookup ON transport_price_ranges (status, effective_date DESC, min_litres, max_litres) WHERE deleted = FALSE;
