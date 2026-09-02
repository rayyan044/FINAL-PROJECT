ALTER TABLE payments ADD COLUMN gateway_status VARCHAR(50);
ALTER TABLE payments ADD COLUMN next_action VARCHAR(50);
ALTER TABLE payments ADD COLUMN authorization_url TEXT;
