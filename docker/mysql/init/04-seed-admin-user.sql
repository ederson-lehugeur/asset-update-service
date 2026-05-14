-- Admin user
-- Email: admin@investalert.com | Password: ****
INSERT INTO `user` (name, email, password_hash, subscription_plan, enabled, created_at, updated_at)
VALUES (
    'Admin',
    'admin@investalert.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'PRO',
    TRUE,
    NOW(6),
    NOW(6)
) ON DUPLICATE KEY UPDATE id = id;

-- Assign ROLE_ADMIN to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM `user` u
JOIN role r ON r.name = 'ROLE_ADMIN'
WHERE u.email = 'admin@investalert.com'
ON DUPLICATE KEY UPDATE user_id = user_id;
