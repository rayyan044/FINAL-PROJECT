-- Database migration: Add username and password_changed columns to users and create audit_logs

-- 1. Add username column as nullable first
ALTER TABLE users ADD COLUMN username VARCHAR(50);

-- 2. Populate username for existing users
UPDATE users SET username = SPLIT_PART(email, '@', 1) WHERE username IS NULL;

-- 3. Resolve any theoretical duplicate usernames
UPDATE users u
SET username = u.username || '_' || u.id
WHERE u.id IN (
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER(PARTITION BY username ORDER BY id) as rn
        FROM users
    ) tmp WHERE rn > 1
);

-- 4. Set username as NOT NULL and add UNIQUE constraint
ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT users_username_key UNIQUE (username);

-- 5. Add password_changed column (default false, not null)
ALTER TABLE users ADD COLUMN password_changed BOOLEAN DEFAULT FALSE NOT NULL;

-- 6. Create audit_logs table
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT,
    admin_username VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    affected_username VARCHAR(100),
    ip_address VARCHAR(45),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details TEXT
);

-- 7. Add index for audit_logs timestamp and admin_username for performance
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_logs_admin ON audit_logs(admin_username);
