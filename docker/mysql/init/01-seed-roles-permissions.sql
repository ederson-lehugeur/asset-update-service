-- Seed roles and permissions for local development
SET NAMES utf8mb4;

-- Roles
INSERT INTO role (name) VALUES
    ('ROLE_ADMIN'),
    ('ROLE_USER')
ON DUPLICATE KEY UPDATE id = id;

-- Permissions
INSERT INTO permission (name) VALUES
    ('ALERT_CREATE'),
    ('ALERT_UPDATE'),
    ('ALERT_DELETE'),
    ('USER_MANAGE'),
    ('SYSTEM_CONFIG')
ON DUPLICATE KEY UPDATE id = id;

-- ROLE_ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.name IN ('ALERT_CREATE', 'ALERT_UPDATE', 'ALERT_DELETE', 'USER_MANAGE', 'SYSTEM_CONFIG')
WHERE r.name = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- ROLE_USER gets alert permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.name IN ('ALERT_CREATE', 'ALERT_UPDATE', 'ALERT_DELETE')
WHERE r.name = 'ROLE_USER'
ON DUPLICATE KEY UPDATE role_id = role_id;
