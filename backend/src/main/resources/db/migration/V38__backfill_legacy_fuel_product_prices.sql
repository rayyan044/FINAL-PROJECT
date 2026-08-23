-- Fuel products created before range-based pricing keep their price in
-- fuel_products.unit_price. Give each such product a default, full-volume
-- range so Finance and the customer portal use the same configured price.
INSERT INTO fuel_price_ranges (
    fuel_product_id,
    min_litres,
    max_litres,
    price_per_litre,
    effective_date,
    status,
    created_by,
    updated_by
)
SELECT
    product.id,
    0.01,
    999999999.99,
    product.unit_price,
    CURRENT_DATE,
    'ACTIVE',
    'System migration',
    'System migration'
FROM fuel_products product
WHERE product.deleted = FALSE
  AND NOT EXISTS (
      SELECT 1
      FROM fuel_price_ranges range
      WHERE range.fuel_product_id = product.id
        AND range.deleted = FALSE
  );
