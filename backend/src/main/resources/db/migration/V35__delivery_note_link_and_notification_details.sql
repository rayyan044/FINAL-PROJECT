-- Add delivery_id column to delivery_notes table with unique constraint (one delivery -> one delivery note)
ALTER TABLE delivery_notes ADD COLUMN IF NOT EXISTS delivery_id BIGINT UNIQUE REFERENCES deliveries(id);

-- Add delivery_id and type columns to notifications table
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS delivery_id BIGINT REFERENCES deliveries(id);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS type VARCHAR(50);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_delivery_notes_delivery ON delivery_notes(delivery_id);
CREATE INDEX IF NOT EXISTS idx_notifications_delivery ON notifications(delivery_id);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);
