ALTER TABLE delivery_notes ADD COLUMN IF NOT EXISTS truck_capacity DECIMAL(12,2);
ALTER TABLE delivery_notes ADD COLUMN IF NOT EXISTS transport_charge DECIMAL(12,2);
ALTER TABLE truck_invoices ADD COLUMN IF NOT EXISTS truck_capacity DECIMAL(12,2);
ALTER TABLE truck_invoices ADD COLUMN IF NOT EXISTS transport_charge DECIMAL(12,2);
