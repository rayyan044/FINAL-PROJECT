-- Payment inherits the common audit and soft-delete fields from BaseEntity.
-- V39 was already applied to existing databases without these columns, so add
-- them in a separate forward-only migration.
ALTER TABLE payments
    ADD COLUMN created_by VARCHAR(255),
    ADD COLUMN updated_by VARCHAR(255),
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at TIMESTAMP;
