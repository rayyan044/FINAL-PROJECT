ALTER TABLE transport_price_ranges
    ADD COLUMN fuel_product_id BIGINT NULL REFERENCES fuel_products(id);

CREATE INDEX idx_transport_price_ranges_fuel_product
    ON transport_price_ranges (fuel_product_id)
    WHERE deleted = FALSE;
