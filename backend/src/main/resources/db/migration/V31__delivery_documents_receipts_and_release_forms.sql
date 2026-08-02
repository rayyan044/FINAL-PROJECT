CREATE TABLE payment_receipts (
    id BIGSERIAL PRIMARY KEY,
    receipt_number VARCHAR(50) NOT NULL UNIQUE,
    invoice_id BIGINT NOT NULL UNIQUE REFERENCES invoices(id),
    receipt_status VARCHAR(30) NOT NULL DEFAULT 'ISSUED',
    received_amount DECIMAL(14,2) NOT NULL,
    received_at TIMESTAMP NOT NULL,
    confirmed_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE TABLE transport_release_forms (
    id BIGSERIAL PRIMARY KEY,
    release_form_number VARCHAR(50) NOT NULL UNIQUE,
    loading_activity_id BIGINT NOT NULL UNIQUE REFERENCES loading_activities(id),
    loading_report_id BIGINT NOT NULL REFERENCES loading_reports(id),
    delivery_note_id BIGINT NOT NULL REFERENCES delivery_notes(id),
    release_status VARCHAR(30) NOT NULL DEFAULT 'PREPARED',
    prepared_at TIMESTAMP NOT NULL,
    prepared_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_payment_receipts_invoice ON payment_receipts(invoice_id);
CREATE INDEX idx_transport_release_forms_activity ON transport_release_forms(loading_activity_id);
