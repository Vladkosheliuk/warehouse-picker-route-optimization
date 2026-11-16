
-- Database roles for Warehouse Picker Route Optimization

BEGIN;

-- Read-only role
CREATE ROLE app_read
    NOLOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE;

-- Application read-write role
CREATE ROLE app_write
    NOLOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE;

COMMIT;
