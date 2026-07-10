-- Create payment_accounts table
CREATE TABLE payment_accounts (
    id BIGSERIAL PRIMARY KEY,
    payment_method VARCHAR(50) NOT NULL,
    beneficiary_name VARCHAR(150) NOT NULL,
    bank_name VARCHAR(150) NOT NULL,
    branch_name VARCHAR(150),
    account_number VARCHAR(100) NOT NULL,
    swift_code VARCHAR(50),
    currency VARCHAR(10) NOT NULL,
    payment_terms VARCHAR(255),
    payment_instructions TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    validity_days INT NOT NULL DEFAULT 30,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Create company_settings table
CREATE TABLE company_settings (
    id BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(150) NOT NULL,
    postal_address VARCHAR(255),
    office_address VARCHAR(255),
    phone_number VARCHAR(50),
    email VARCHAR(100),
    logo VARCHAR(255),
    signatory_name VARCHAR(150),
    signatory_title VARCHAR(150),
    signatory_signature VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Add snapshot fields to invoices
ALTER TABLE invoices ADD COLUMN payment_account_id BIGINT;
ALTER TABLE invoices ADD COLUMN payment_method VARCHAR(50);
ALTER TABLE invoices ADD COLUMN beneficiary_name VARCHAR(150);
ALTER TABLE invoices ADD COLUMN bank_name VARCHAR(150);
ALTER TABLE invoices ADD COLUMN branch_name VARCHAR(150);
ALTER TABLE invoices ADD COLUMN account_number VARCHAR(100);
ALTER TABLE invoices ADD COLUMN swift_code VARCHAR(50);
ALTER TABLE invoices ADD COLUMN payment_account_currency VARCHAR(10);
ALTER TABLE invoices ADD COLUMN payment_terms VARCHAR(255);
ALTER TABLE invoices ADD COLUMN payment_instructions TEXT;
ALTER TABLE invoices ADD COLUMN validity_date TIMESTAMP;

-- Add currency fields to fuel_products and fuel_orders
ALTER TABLE fuel_products ADD COLUMN currency VARCHAR(10) DEFAULT 'USD';
ALTER TABLE fuel_orders ADD COLUMN currency VARCHAR(10) DEFAULT 'USD';
