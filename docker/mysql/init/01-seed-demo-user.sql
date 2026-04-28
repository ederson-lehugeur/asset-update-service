-- Demo user for local development
-- Email: demo@investmonitor.com | Password: demo123
INSERT INTO `user` (name, email, password_hash, created_at, updated_at)
VALUES (
    'Demo User',
    'demo@investalert.com',
    '$2a$10$L7YEmZLFsKCEt3y.kuO0SuwsjtGaSrhUpDf0iGabZ4Ehzx4sx/O8u',
    NOW(6),
    NOW(6)
) ON DUPLICATE KEY UPDATE id = id;
