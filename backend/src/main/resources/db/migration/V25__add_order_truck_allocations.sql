CREATE TABLE order_truck_allocations (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES fuel_orders(id),
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id),
    allocated_quantity NUMERIC(12,2) NOT NULL,
    capacity_snapshot NUMERIC(12,2) NOT NULL,
    transport_price NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    CONSTRAINT uk_order_truck_allocations_order_vehicle UNIQUE (order_id, vehicle_id)
);
CREATE INDEX idx_order_truck_allocations_order ON order_truck_allocations(order_id);
