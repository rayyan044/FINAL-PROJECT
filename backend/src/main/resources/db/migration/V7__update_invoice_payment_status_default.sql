-- Update default value for payment_status to 'PENDING_PAYMENT'
ALTER TABLE invoices ALTER COLUMN payment_status SET DEFAULT 'PENDING_PAYMENT';

-- Update existing records from 'PENDING' to 'PENDING_PAYMENT'
UPDATE invoices SET payment_status = 'PENDING_PAYMENT' WHERE payment_status = 'PENDING';
