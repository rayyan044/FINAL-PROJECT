CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE transport_price_ranges
    DROP CONSTRAINT IF EXISTS transport_price_ranges_no_overlap;

ALTER TABLE transport_price_ranges
    ADD CONSTRAINT transport_price_ranges_no_overlap_per_product
    EXCLUDE USING gist (
        fuel_product_id WITH =,
        numrange(min_litres, max_litres, '[]') WITH &&
    ) WHERE (deleted = FALSE);
