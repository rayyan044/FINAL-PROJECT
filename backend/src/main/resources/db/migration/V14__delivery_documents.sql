-- Create delivery_notes table
CREATE TABLE delivery_notes (
    id BIGSERIAL PRIMARY KEY,
    delivery_note_number VARCHAR(50) NOT NULL UNIQUE,
    loading_order_id BIGINT REFERENCES loading_orders(id),
    loading_activity_id BIGINT REFERENCES loading_activities(id),
    loading_report_id BIGINT REFERENCES loading_reports(id),
    customer_id BIGINT REFERENCES customers(id),
    product_id BIGINT REFERENCES fuel_products(id),
    
    truck_number VARCHAR(50),
    driver_name VARCHAR(150),
    driver_license_number VARCHAR(100),
    transport_company VARCHAR(150),
    
    ambient_volume DECIMAL(12,2),
    standard_volume DECIMAL(12,2),
    
    destination VARCHAR(255),
    status VARCHAR(50) NOT NULL, -- PREPARED, PRINTED, HANDED_TO_DRIVER
    
    prepared_by VARCHAR(100),
    prepared_at TIMESTAMP,
    printed_by VARCHAR(100),
    printed_at TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Create truck_invoices table
CREATE TABLE truck_invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    loading_order_id BIGINT REFERENCES loading_orders(id),
    loading_activity_id BIGINT REFERENCES loading_activities(id),
    delivery_note_id BIGINT REFERENCES delivery_notes(id),
    customer_id BIGINT REFERENCES customers(id),
    product_id BIGINT REFERENCES fuel_products(id),
    
    truck_number VARCHAR(50),
    driver_name VARCHAR(150),
    transport_company VARCHAR(150),
    
    quantity DECIMAL(12,2),
    unit_price DECIMAL(12,2),
    total_amount DECIMAL(14,2),
    
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PAID',
    invoice_status VARCHAR(50) NOT NULL, -- GENERATED, PRINTED
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_delivery_notes_activity ON delivery_notes(loading_activity_id);
CREATE INDEX idx_delivery_notes_order ON delivery_notes(loading_order_id);
CREATE INDEX idx_delivery_notes_report ON delivery_notes(loading_report_id);
CREATE INDEX idx_delivery_notes_customer ON delivery_notes(customer_id);
CREATE INDEX idx_delivery_notes_status ON delivery_notes(status);

CREATE INDEX idx_truck_invoices_activity ON truck_invoices(loading_activity_id);
CREATE INDEX idx_truck_invoices_order ON truck_invoices(loading_order_id);
CREATE INDEX idx_truck_invoices_dn ON truck_invoices(delivery_note_id);
CREATE INDEX idx_truck_invoices_customer ON truck_invoices(customer_id);
CREATE INDEX idx_truck_invoices_status ON truck_invoices(invoice_status);
