-- Links portal users to the company record that owns their commercial data.
-- Nullable because staff and driver accounts are not customers.
ALTER TABLE users ADD COLUMN IF NOT EXISTS customer_id BIGINT REFERENCES customers(id);
CREATE INDEX IF NOT EXISTS idx_users_customer_id ON users(customer_id);
