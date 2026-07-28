-- Expand fuel_products with thermal_expansion_coefficient
ALTER TABLE fuel_products ADD COLUMN thermal_expansion_coefficient DECIMAL(6,5) DEFAULT 0.00084 NOT NULL;

-- Populate default values for thermal expansion coefficients
UPDATE fuel_products SET thermal_expansion_coefficient = 0.00120 WHERE product_name ILIKE '%PMS%' OR product_name ILIKE '%petrol%' OR product_name ILIKE '%gasoline%';
UPDATE fuel_products SET thermal_expansion_coefficient = 0.00084 WHERE product_name ILIKE '%AGO%' OR product_name ILIKE '%diesel%';

-- Expand loading_activities with physical measurements and completed details
ALTER TABLE loading_activities ADD COLUMN ambient_volume DECIMAL(12,2);
ALTER TABLE loading_activities ADD COLUMN temperature DECIMAL(5,2);
ALTER TABLE loading_activities ADD COLUMN density DECIMAL(6,4);
ALTER TABLE loading_activities ADD COLUMN standard_volume DECIMAL(12,2);
ALTER TABLE loading_activities ADD COLUMN meter_start DECIMAL(12,2);
ALTER TABLE loading_activities ADD COLUMN meter_end DECIMAL(12,2);
ALTER TABLE loading_activities ADD COLUMN meter_difference DECIMAL(12,2);
ALTER TABLE loading_activities ADD COLUMN remarks TEXT;
ALTER TABLE loading_activities ADD COLUMN completed_by BIGINT REFERENCES users(id);
ALTER TABLE loading_activities ADD COLUMN completed_at TIMESTAMP;

-- Create loading_compartments table
CREATE TABLE loading_compartments (
    id BIGSERIAL PRIMARY KEY,
    loading_activity_id BIGINT NOT NULL REFERENCES loading_activities(id),
    compartment_number INTEGER NOT NULL,
    capacity DECIMAL(12,2) NOT NULL,
    product_id BIGINT NOT NULL REFERENCES fuel_products(id),
    product_name_snapshot VARCHAR(100) NOT NULL,
    product_code_snapshot VARCHAR(50),
    ambient_volume DECIMAL(12,2) NOT NULL,
    temperature DECIMAL(5,2) NOT NULL,
    density DECIMAL(6,4) NOT NULL,
    standard_volume DECIMAL(12,2) NOT NULL,
    seal_number VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Create inventory_transactions table
CREATE TABLE inventory_transactions (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES fuel_products(id),
    quantity DECIMAL(12,2) NOT NULL,
    stock_before DECIMAL(12,2) NOT NULL,
    stock_after DECIMAL(12,2) NOT NULL,
    movement_type VARCHAR(50) NOT NULL, -- LOADING, RECEIPT, ADJUSTMENT
    movement_reason VARCHAR(255),
    performed_by BIGINT REFERENCES users(id),
    reference_id BIGINT,
    reference_type VARCHAR(50), -- LOADING_ACTIVITY
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Create loading_reports table
CREATE TABLE loading_reports (
    id BIGSERIAL PRIMARY KEY,
    loading_activity_id BIGINT NOT NULL REFERENCES loading_activities(id),
    loading_order_id BIGINT NOT NULL REFERENCES loading_orders(id),
    report_number VARCHAR(50) NOT NULL UNIQUE,
    loading_officer VARCHAR(150) NOT NULL,
    terminal VARCHAR(150) NOT NULL,
    loading_bay VARCHAR(50) NOT NULL,
    report_status VARCHAR(30) NOT NULL, -- GENERATED, CANCELLED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Add indexes
CREATE INDEX idx_loading_compartments_activity ON loading_compartments(loading_activity_id);
CREATE INDEX idx_inventory_transactions_product ON inventory_transactions(product_id);
CREATE INDEX idx_loading_reports_activity ON loading_reports(loading_activity_id);
CREATE INDEX idx_loading_reports_order ON loading_reports(loading_order_id);
