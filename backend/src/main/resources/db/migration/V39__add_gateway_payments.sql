CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    payment_reference VARCHAR(100) NOT NULL UNIQUE,
    gateway VARCHAR(30) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    phone_number VARCHAR(20),
    amount NUMERIC(14,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL,
    gateway_transaction_id VARCHAR(150) UNIQUE,
    failure_reason TEXT,
    initiated_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_payments_invoice_id ON payments(invoice_id);
