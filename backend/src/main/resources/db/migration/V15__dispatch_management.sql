-- Create dispatches table
CREATE TABLE dispatches (
    id BIGSERIAL PRIMARY KEY,
    dispatch_number VARCHAR(50) NOT NULL UNIQUE,
    loading_order_id BIGINT REFERENCES loading_orders(id),
    loading_activity_id BIGINT REFERENCES loading_activities(id),
    delivery_note_id BIGINT REFERENCES delivery_notes(id),
    truck_invoice_id BIGINT REFERENCES truck_invoices(id),
    
    truck_number VARCHAR(50),
    driver_name VARCHAR(150),
    driver_license_number VARCHAR(100),
    transport_company VARCHAR(150),
    
    destination VARCHAR(255),
    
    dispatch_officer VARCHAR(150),
    departure_time TIMESTAMP,
    released_by VARCHAR(150),
    released_at TIMESTAMP,
    
    dispatch_status VARCHAR(50) NOT NULL, -- READY, DISPATCHED, IN_TRANSIT, CANCELLED
    remarks TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_dispatches_activity ON dispatches(loading_activity_id);
CREATE INDEX idx_dispatches_order ON dispatches(loading_order_id);
CREATE INDEX idx_dispatches_dn ON dispatches(delivery_note_id);
CREATE INDEX idx_dispatches_invoice ON dispatches(truck_invoice_id);
CREATE INDEX idx_dispatches_status ON dispatches(dispatch_status);
