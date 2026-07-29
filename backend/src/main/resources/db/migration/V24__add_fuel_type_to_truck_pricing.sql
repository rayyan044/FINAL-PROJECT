ALTER TABLE truck_pricing ADD COLUMN IF NOT EXISTS fuel_type VARCHAR(50) NOT NULL DEFAULT 'ALL';
ALTER TABLE truck_pricing DROP CONSTRAINT IF EXISTS truck_pricing_capacity_key;
ALTER TABLE truck_pricing DROP CONSTRAINT IF EXISTS uke8mjjevij9ydjq0yik3e6d1yo;
CREATE UNIQUE INDEX IF NOT EXISTS uk_truck_pricing_capacity_fuel_type ON truck_pricing(capacity, fuel_type);
