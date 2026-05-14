-- Demo user for local development
-- Email: demo@investalert.com | Password: demo123
INSERT INTO `user` (name, email, password_hash, subscription_plan, enabled, created_at, updated_at)
VALUES (
    'Demo User',
    'demo@investalert.com',
    '$2a$10$L7YEmZLFsKCEt3y.kuO0SuwsjtGaSrhUpDf0iGabZ4Ehzx4sx/O8u',
    'FREE',
    TRUE,
    NOW(6),
    NOW(6)
) ON DUPLICATE KEY UPDATE id = id;

-- Assign ROLE_USER to demo user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM `user` u
JOIN role r ON r.name = 'ROLE_USER'
WHERE u.email = 'demo@investalert.com'
ON DUPLICATE KEY UPDATE user_id = user_id;
