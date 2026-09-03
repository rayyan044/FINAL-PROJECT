ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS mobile_money_network VARCHAR(30),
    ADD COLUMN IF NOT EXISTS provider_reference VARCHAR(150),
    ADD COLUMN IF NOT EXISTS authorization_instruction TEXT,
    ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_checked_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS payment_webhook_events (
    id BIGSERIAL PRIMARY KEY,
    gateway VARCHAR(30) NOT NULL,
    event_id VARCHAR(150) NOT NULL,
    event_type VARCHAR(100),
    payment_id BIGINT REFERENCES payments(id),
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (gateway, event_id)
);
CREATE INDEX IF NOT EXISTS idx_payment_webhook_events_payment_id ON payment_webhook_events(payment_id);
