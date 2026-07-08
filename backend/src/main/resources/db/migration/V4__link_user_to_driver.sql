-- V4__link_user_to_driver.sql
-- Add nullable driver_id column to users table and set up unique constraint and foreign key

ALTER TABLE users ADD COLUMN driver_id BIGINT;

ALTER TABLE users ADD CONSTRAINT fk_users_driver FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE SET NULL;

ALTER TABLE users ADD CONSTRAINT uq_users_driver UNIQUE (driver_id);
