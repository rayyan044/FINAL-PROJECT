-- Drop the default unique constraint if it exists
ALTER TABLE fuel_products DROP CONSTRAINT IF EXISTS fuel_products_product_name_key;

-- Drop the default unique index if it exists
DROP INDEX IF EXISTS fuel_products_product_name_key;

-- Create case-insensitive unique index on active (non-deleted) fuel products
CREATE UNIQUE INDEX IF NOT EXISTS fuel_products_product_name_active_idx ON fuel_products (LOWER(product_name)) WHERE (deleted = false);
