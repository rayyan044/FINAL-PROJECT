-- Drop existing deliveries table to recreate it clean
DROP TABLE IF EXISTS deliveries CASCADE;

-- Recreate deliveries table
CREATE TABLE deliveries (
    id BIGSERIAL PRIMARY KEY,
    delivery_number VARCHAR(50) NOT NULL UNIQUE,
    dispatch_id BIGINT REFERENCES dispatches(id),
    loading_order_id BIGINT REFERENCES loading_orders(id),
    loading_activity_id BIGINT REFERENCES loading_activities(id),
    delivery_note_id BIGINT REFERENCES delivery_notes(id),
    truck_invoice_id BIGINT REFERENCES truck_invoices(id),
    
    truck_number VARCHAR(50),
    driver_name VARCHAR(150),
    transport_company VARCHAR(150),
    
    destination VARCHAR(255),
    delivery_status VARCHAR(50) NOT NULL, -- IN_TRANSIT, ARRIVED_AT_DESTINATION, DELIVERED, CANCELLED
    
    dispatched_at TIMESTAMP,
    arrival_time TIMESTAMP,
    delivered_at TIMESTAMP,
    
    received_by VARCHAR(150),
    completed_by VARCHAR(150),
    remarks TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Recreate indexes
CREATE INDEX idx_deliveries_dispatch ON deliveries(dispatch_id);
CREATE INDEX idx_deliveries_activity ON deliveries(loading_activity_id);
CREATE INDEX idx_deliveries_status ON deliveries(delivery_status);
