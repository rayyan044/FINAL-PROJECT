-- Add columns for original requested quantity, approved quantity and reason to fuel_orders
ALTER TABLE fuel_orders ADD COLUMN original_quantity DECIMAL(12,2);
ALTER TABLE fuel_orders ADD COLUMN approved_quantity DECIMAL(12,2);
ALTER TABLE fuel_orders ADD COLUMN edit_reason VARCHAR(255);

-- Create invoices table
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    invoice_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    order_id BIGINT NOT NULL REFERENCES fuel_orders(id),
    subtotal DECIMAL(12,2) NOT NULL,
    tax DECIMAL(12,2) NOT NULL,
    grand_total DECIMAL(12,2) NOT NULL,
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    finance_approved_by VARCHAR(100),
    finance_approved_at TIMESTAMP,
    terms_and_conditions TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Index for invoice search and order lookups
CREATE INDEX idx_invoices_number ON invoices(invoice_number);
CREATE INDEX idx_invoices_order ON invoices(order_id);
