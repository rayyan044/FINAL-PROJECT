-- Add specifications and categories to fuel_products
ALTER TABLE fuel_products ADD COLUMN fuel_category VARCHAR(100);
ALTER TABLE fuel_products ADD COLUMN specification TEXT;
ALTER TABLE fuel_products ADD COLUMN description TEXT;
ALTER TABLE fuel_products ADD COLUMN unit_of_measurement VARCHAR(50) DEFAULT 'Litres';

-- Add additional charges and delivery details to fuel_orders
ALTER TABLE fuel_orders ADD COLUMN levies DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE fuel_orders ADD COLUMN discount DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE fuel_orders ADD COLUMN transport_charges DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE fuel_orders ADD COLUMN delivery_charges DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE fuel_orders ADD COLUMN additional_charges DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE fuel_orders ADD COLUMN delivery_method VARCHAR(100);
ALTER TABLE fuel_orders ADD COLUMN incoterms VARCHAR(100);
ALTER TABLE fuel_orders ADD COLUMN port VARCHAR(100);
ALTER TABLE fuel_orders ADD COLUMN destination VARCHAR(150);
ALTER TABLE fuel_orders ADD COLUMN logistics_info TEXT;

-- Add stamp to company_settings
ALTER TABLE company_settings ADD COLUMN stamp VARCHAR(255);

-- Add calculation, product details, and delivery snapshot fields to invoices
ALTER TABLE invoices ADD COLUMN fuel_category VARCHAR(100);
ALTER TABLE invoices ADD COLUMN product_specification TEXT;
ALTER TABLE invoices ADD COLUMN product_description TEXT;
ALTER TABLE invoices ADD COLUMN unit_of_measurement VARCHAR(50);
ALTER TABLE invoices ADD COLUMN levies DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE invoices ADD COLUMN discount DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE invoices ADD COLUMN transport_charges DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE invoices ADD COLUMN delivery_charges DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE invoices ADD COLUMN additional_charges DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE invoices ADD COLUMN delivery_method VARCHAR(100);
ALTER TABLE invoices ADD COLUMN incoterms VARCHAR(100);
ALTER TABLE invoices ADD COLUMN port VARCHAR(100);
ALTER TABLE invoices ADD COLUMN destination VARCHAR(150);
ALTER TABLE invoices ADD COLUMN logistics_info TEXT;
