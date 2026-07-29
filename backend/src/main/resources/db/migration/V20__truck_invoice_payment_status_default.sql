-- Truck invoices enter Finance's normal payment approval workflow.
ALTER TABLE truck_invoices ALTER COLUMN payment_status SET DEFAULT 'PENDING_PAYMENT';
