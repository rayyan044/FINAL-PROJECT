-- Create Tables with JPA Auditing and Soft Delete columns

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    customer_code VARCHAR(50) NOT NULL UNIQUE,
    company_name VARCHAR(150) NOT NULL,
    contact_person VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100) UNIQUE,
    address TEXT,
    tin_number VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE TABLE fuel_products (
    id BIGSERIAL PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL UNIQUE,
    fuel_type VARCHAR(50) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    density DECIMAL(8,4) NOT NULL,
    available_quantity DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE TABLE storage_tanks (
    id BIGSERIAL PRIMARY KEY,
    tank_name VARCHAR(100) NOT NULL UNIQUE,
    capacity DECIMAL(12,2) NOT NULL,
    current_volume DECIMAL(12,2) NOT NULL,
    fuel_product_id BIGINT NOT NULL REFERENCES fuel_products(id),
    location VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE TABLE fuel_orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    product_id BIGINT NOT NULL REFERENCES fuel_products(id),
    quantity DECIMAL(12,2) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivery_date TIMESTAMP,
    order_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE TABLE drivers (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) UNIQUE,
    license_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    plate_number VARCHAR(30) NOT NULL UNIQUE,
    capacity DECIMAL(12,2) NOT NULL,
    current_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    driver_id BIGINT UNIQUE REFERENCES drivers(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE TABLE deliveries (
    id BIGSERIAL PRIMARY KEY,
    delivery_number VARCHAR(50) NOT NULL UNIQUE,
    driver_id BIGINT NOT NULL REFERENCES drivers(id),
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id),
    order_id BIGINT NOT NULL REFERENCES fuel_orders(id),
    delivery_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    departure_time TIMESTAMP,
    arrival_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE TABLE fuel_transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_number VARCHAR(50) NOT NULL UNIQUE,
    product_id BIGINT NOT NULL REFERENCES fuel_products(id),
    quantity DECIMAL(12,2) NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE TABLE inventories (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES fuel_products(id),
    opening_stock DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    received_stock DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    issued_stock DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    closing_stock DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    record_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Search Indexes for high-frequency queries
CREATE INDEX idx_customers_code ON customers(customer_code);
CREATE INDEX idx_customers_name ON customers(company_name);
CREATE INDEX idx_fuel_orders_number ON fuel_orders(order_number);
CREATE INDEX idx_fuel_orders_status ON fuel_orders(order_status);
CREATE INDEX idx_fuel_products_name ON fuel_products(product_name);
CREATE INDEX idx_vehicles_plate ON vehicles(plate_number);
CREATE INDEX idx_drivers_license ON drivers(license_number);
CREATE INDEX idx_deliveries_status ON deliveries(delivery_status);
CREATE INDEX idx_inventories_product_date ON inventories(product_id, record_date);
