-- Database Migration: V18__system_administration.sql
-- Implement central roles, permissions, role_permissions, system_settings, and update users & audit_logs

-- 1. Create roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed default roles
INSERT INTO roles (role_name, description) VALUES
('ADMIN', 'Administrator with full system access'),
('MANAGER', 'Manager with access to view reports and operational details'),
('SALES_OFFICER', 'Sales officer for fuel requests and quotations'),
('FINANCE', 'Finance officer for invoicing and payments'),
('OPERATIONS', 'Operations officer for loading and routing'),
('OPERATOR', 'Depot/Terminal operator'),
('DISPATCHER', 'Dispatcher for vehicle loading and releases'),
('CUSTOMER_SERVICE', 'Customer service representative'),
('VIEWER', 'Viewer with read-only access')
ON CONFLICT (role_name) DO NOTHING;

-- 2. Create permissions table
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    permission_name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed default permissions
INSERT INTO permissions (permission_name, description) VALUES
('USER_CREATE', 'Permission to create new user accounts'),
('USER_UPDATE', 'Permission to update user accounts'),
('USER_DELETE', 'Permission to delete user accounts'),
('VIEW_REPORTS', 'Permission to view analytics and reports'),
('MANAGE_SETTINGS', 'Permission to manage system configurations'),
('VIEW_AUDIT', 'Permission to view system audit logs'),
('CONFIRM_PAYMENT', 'Permission to approve/confirm payments'),
('MANAGE_INVENTORY', 'Permission to manage inventory and storage tanks'),
('MANAGE_LOADING', 'Permission to manage loading orders and activities'),
('MANAGE_DISPATCH', 'Permission to manage dispatch releases'),
('MANAGE_DELIVERY', 'Permission to manage delivery reports and status updates')
ON CONFLICT (permission_name) DO NOTHING;

-- 3. Create role_permissions table
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT unique_role_permission UNIQUE (role_id, permission_id)
);

-- Map all permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.role_name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Map specific permissions to MANAGER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.role_name = 'MANAGER' AND p.permission_name IN ('VIEW_REPORTS', 'VIEW_AUDIT')
ON CONFLICT DO NOTHING;

-- Map specific permissions to SALES_OFFICER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.role_name = 'SALES_OFFICER' AND p.permission_name IN ('VIEW_REPORTS')
ON CONFLICT DO NOTHING;

-- Map specific permissions to FINANCE role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.role_name = 'FINANCE' AND p.permission_name IN ('CONFIRM_PAYMENT', 'VIEW_REPORTS')
ON CONFLICT DO NOTHING;

-- Map specific permissions to OPERATIONS role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.role_name = 'OPERATIONS' AND p.permission_name IN ('MANAGE_LOADING', 'MANAGE_DELIVERY', 'VIEW_REPORTS')
ON CONFLICT DO NOTHING;

-- Map specific permissions to OPERATOR role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.role_name = 'OPERATOR' AND p.permission_name IN ('MANAGE_INVENTORY', 'MANAGE_LOADING')
ON CONFLICT DO NOTHING;

-- Map specific permissions to DISPATCHER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.role_name = 'DISPATCHER' AND p.permission_name IN ('MANAGE_DISPATCH')
ON CONFLICT DO NOTHING;

-- Map specific permissions to CUSTOMER_SERVICE role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.role_name = 'CUSTOMER_SERVICE' AND p.permission_name IN ('VIEW_REPORTS')
ON CONFLICT DO NOTHING;

-- 4. Update users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(150);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS role_id BIGINT REFERENCES roles(id);

-- Populate full_name from existing columns
UPDATE users SET full_name = COALESCE(first_name, '') || ' ' || COALESCE(last_name, '') WHERE full_name IS NULL;

-- Link existing role string to new role_id reference
UPDATE users u
SET role_id = r.id
FROM roles r
WHERE u.role = r.role_name AND u.role_id IS NULL;

-- Assign default role (VIEWER) if role_id is still null
UPDATE users u
SET role_id = (SELECT id FROM roles WHERE role_name = 'VIEWER')
WHERE u.role_id IS NULL;

-- 5. Create system_settings table
CREATE TABLE IF NOT EXISTS system_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) UNIQUE NOT NULL,
    setting_value TEXT NOT NULL,
    description VARCHAR(255),
    updated_by VARCHAR(100) DEFAULT 'SYSTEM',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed default settings
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('COMPANY_NAME', 'FALCON ENERGY LIMITED', 'Official name of the company'),
('COMPANY_ADDRESS', 'P.O. Box : 45431, 6th Floor, SALAMANDER TOWER, SAMORA AVENUE, DAR ES SALAAM', 'Registered postal and office address'),
('COMPANY_PHONE', '+255 22 212 3456', 'Corporate contact number'),
('INVOICE_PREFIX', 'INV-TRUCK-', 'Prefix for generated truck invoice numbers'),
('DELIVERY_NOTE_PREFIX', 'DN-', 'Prefix for generated delivery note numbers'),
('REPORT_PREFIX', 'REP-', 'Prefix for system reports'),
('CURRENCY', 'USD', 'Default operating currency')
ON CONFLICT (setting_key) DO NOTHING;

-- 6. Update audit_logs table
-- Rather than drop/recreate, we drop/alter to match the required columns while ensuring data mapping is safe
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS username VARCHAR(100);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS module VARCHAR(100);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS old_value TEXT;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Copy any existing admin_username into username, and timestamp into created_at
UPDATE audit_logs SET username = admin_username WHERE username IS NULL;
UPDATE audit_logs SET created_at = timestamp WHERE created_at IS NULL;
UPDATE audit_logs SET old_value = previous_value WHERE old_value IS NULL;
UPDATE audit_logs SET module = COALESCE(entity_type, 'SYSTEM') WHERE module IS NULL;
