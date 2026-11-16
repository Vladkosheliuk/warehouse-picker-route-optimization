
-- Technical database account for Spring Boot

BEGIN;

CREATE ROLE warehouse_app
    LOGIN
    INHERIT
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOREPLICATION
    NOBYPASSRLS;

GRANT app_write TO warehouse_app;

COMMIT;
