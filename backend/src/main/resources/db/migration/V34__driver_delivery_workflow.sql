-- Add columns to deliveries for trip tracking and Proof of Delivery (POD)
ALTER TABLE deliveries
ADD COLUMN start_latitude DOUBLE PRECISION,
ADD COLUMN start_longitude DOUBLE PRECISION,
ADD COLUMN pod_latitude DOUBLE PRECISION,
ADD COLUMN pod_longitude DOUBLE PRECISION,
ADD COLUMN pod_photo_path VARCHAR(255),
ADD COLUMN pod_notes TEXT,
ADD COLUMN pod_uploaded_at TIMESTAMP;

-- Create notifications table for driver notifications
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_unread ON notifications(user_id) WHERE is_read = FALSE;
