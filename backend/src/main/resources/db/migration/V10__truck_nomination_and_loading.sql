-- Create truck_nominations table
CREATE TABLE truck_nominations (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE REFERENCES fuel_orders(id),
    transport_source VARCHAR(50) NOT NULL, -- CUSTOMER_TRUCKS, FALCON_ARRANGED
    number_of_trucks INTEGER,
    confirmation_notes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', -- DRAFT, SUBMITTED
    total_allocated_quantity DECIMAL(12,2) DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Create truck_nomination_items table
CREATE TABLE truck_nomination_items (
    id BIGSERIAL PRIMARY KEY,
    nomination_id BIGINT NOT NULL REFERENCES truck_nominations(id),
    truck_number VARCHAR(50) NOT NULL,
    trailer_number VARCHAR(50) NOT NULL,
    driver_name VARCHAR(150) NOT NULL,
    driver_licence_number VARCHAR(50) NOT NULL,
    driver_passport VARCHAR(100),
    transport_company VARCHAR(150) NOT NULL,
    destination VARCHAR(150) NOT NULL,
    truck_capacity DECIMAL(12,2) NOT NULL,
    allocated_quantity DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Create loading_orders table
CREATE TABLE loading_orders (
    id BIGSERIAL PRIMARY KEY,
    loading_order_number VARCHAR(50) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL UNIQUE REFERENCES fuel_orders(id),
    loading_date DATE NOT NULL,
    loading_terminal VARCHAR(150) NOT NULL,
    consignee VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', -- DRAFT, APPROVED, LOADING_IN_PROGRESS, COMPLETED, CANCELLED
    prepared_by VARCHAR(100),
    approved_by VARCHAR(100),
    loading_remarks TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Create loading_activities table
CREATE TABLE loading_activities (
    id BIGSERIAL PRIMARY KEY,
    loading_order_id BIGINT NOT NULL REFERENCES loading_orders(id),
    truck_number VARCHAR(50) NOT NULL,
    driver_name VARCHAR(150) NOT NULL,
    driver_licence_number VARCHAR(50) NOT NULL,
    transport_company VARCHAR(150) NOT NULL,
    destination VARCHAR(150) NOT NULL,
    product VARCHAR(100) NOT NULL,
    allocated_quantity DECIMAL(12,2) NOT NULL,
    queue_number VARCHAR(50) NOT NULL,
    bay_number VARCHAR(50) NOT NULL,
    pump_number VARCHAR(50),
    loading_start_time TIMESTAMP,
    loading_completion_time TIMESTAMP,
    loading_officer VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING', -- WAITING, LOADING, LOADED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Add indexes
CREATE INDEX idx_truck_nominations_order ON truck_nominations(order_id);
CREATE INDEX idx_truck_nominations_status ON truck_nominations(status);
CREATE INDEX idx_loading_orders_number ON loading_orders(loading_order_number);
CREATE INDEX idx_loading_orders_order ON loading_orders(order_id);
CREATE INDEX idx_loading_orders_status ON loading_orders(status);
CREATE INDEX idx_loading_activities_order ON loading_activities(loading_order_id);
CREATE INDEX idx_loading_activities_status ON loading_activities(status);
