-- =============================================================
-- Oracle HRMS development admin seed
-- WARNING: DEVELOPMENT ONLY
-- =============================================================

INSERT INTO app_user (
    user_id,
    username,
    password_hash,
    full_name,
    role,
    is_active,
    created_at,
    updated_at,
    last_login_at,
    created_by
) VALUES (
    app_user_seq.NEXTVAL,
    'admin',
    'Admin@123',
    'System Admin',
    'SUPER_ADMIN',
    1,
    SYSTIMESTAMP,
    SYSTIMESTAMP,
    NULL,
    'SYSTEM'
);

COMMIT;
PROMPT Development admin user seeded. Username: admin | Password: Admin@123 (DEVELOPMENT ONLY).
