
-- Verify database roles and permissions

-- 1. Verify role attributes

SELECT
    rolname AS role_name,
    rolsuper AS is_superuser,
    rolcreaterole AS can_create_roles,
    rolcreatedb AS can_create_database,
    rolcanlogin AS can_login
FROM pg_roles
WHERE rolname IN ('postgres', 'app_read', 'app_write')
ORDER BY rolname;


-- 2. Verify read-only permissions

SELECT
    has_table_privilege(
            'app_read', 'public.products', 'SELECT'
    ) AS can_select,
    has_table_privilege(
            'app_read', 'public.products', 'INSERT'
    ) AS can_insert,
    has_table_privilege(
            'app_read', 'public.products', 'UPDATE'
    ) AS can_update,
    has_table_privilege(
            'app_read', 'public.products', 'DELETE'
    ) AS can_delete;


-- 3. Verify application permissions

SELECT
    has_table_privilege(
            'app_write', 'public.products', 'SELECT'
    ) AS can_select,
    has_table_privilege(
            'app_write', 'public.products', 'INSERT'
    ) AS can_insert,
    has_table_privilege(
            'app_write', 'public.products', 'UPDATE'
    ) AS can_update,
    has_table_privilege(
            'app_write', 'public.products', 'DELETE'
    ) AS can_delete;


-- 4. Verify password hash protection

SELECT
    has_column_privilege(
            'app_read', 'public.users', 'password_hash', 'SELECT'
    ) AS reader_can_read_password_hash,
    has_column_privilege(
            'app_write', 'public.users', 'password_hash', 'SELECT'
    ) AS application_can_read_password_hash;


-- 5. Verify schema permissions

SELECT
    has_schema_privilege(
            'app_read', 'public', 'CREATE'
    ) AS reader_can_create_tables,
    has_schema_privilege(
            'app_write', 'public', 'CREATE'
    ) AS application_can_create_tables;


-- 6. Verify migration history protection

SELECT
    has_table_privilege(
            'app_read', 'public.flyway_schema_history', 'SELECT'
    ) AS reader_can_access_migrations,
    has_table_privilege(
            'app_write', 'public.flyway_schema_history', 'UPDATE'
    ) AS application_can_modify_migrations;
