
-- Database permissions for Warehouse Picker Route Optimization
-- PostgreSQL / Supabase
--
-- postgres  : database administration and migrations
-- app_read  : read-only access
-- app_write : application data access

BEGIN;

-- =============================================
-- 1. Schema access
-- =============================================

GRANT USAGE ON SCHEMA public TO app_read;
GRANT USAGE ON SCHEMA public TO app_write;

-- Prevent application roles from creating objects

REVOKE CREATE ON SCHEMA public FROM app_read;
REVOKE CREATE ON SCHEMA public FROM app_write;

-- =============================================
-- 2. Read-only permissions
-- =============================================

GRANT SELECT ON TABLE
    public.warehouses,
    public.warehouse_nodes,
    public.warehouse_edges,
    public.products,
    public.product_locations,
    public.picking_tasks,
    public.picking_task_items,
    public.picking_routes,
    public.route_stops
    TO app_read;

-- Allow reading user information without password hashes

GRANT SELECT (
              id,
              username,
              full_name,
              role,
              is_active,
              created_at
    )
    ON public.users TO app_read;

-- =============================================
-- 3. Application read-write permissions
-- =============================================

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE
    public.warehouses,
    public.warehouse_nodes,
    public.warehouse_edges,
    public.products,
    public.product_locations,
    public.picking_tasks,
    public.picking_task_items,
    public.picking_routes,
    public.route_stops
    TO app_write;

-- User account management and authentication

GRANT SELECT, INSERT, UPDATE, DELETE
    ON public.users TO app_write;

-- =============================================
-- 4. Sequence permissions
-- =============================================

-- Required for generating new identity IDs

GRANT USAGE, SELECT ON SEQUENCE
    public.warehouses_id_seq,
    public.warehouse_nodes_id_seq,
    public.warehouse_edges_id_seq,
    public.products_id_seq,
    public.product_locations_id_seq,
    public.users_id_seq,
    public.picking_tasks_id_seq,
    public.picking_task_items_id_seq,
    public.picking_routes_id_seq,
    public.route_stops_id_seq
    TO app_write;

-- =============================================
-- 5. Protect Flyway migration history
-- =============================================

REVOKE ALL PRIVILEGES
    ON public.flyway_schema_history
    FROM app_read, app_write;

-- =============================================
-- 6. Default privileges for future objects
-- =============================================

-- New tables must receive explicit permissions.
-- This prevents accidental access to sensitive data.

ALTER DEFAULT PRIVILEGES FOR ROLE postgres
IN SCHEMA public
REVOKE ALL ON TABLES FROM PUBLIC;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres
IN SCHEMA public
REVOKE ALL ON SEQUENCES FROM PUBLIC;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres
IN SCHEMA public
GRANT USAGE, SELECT ON SEQUENCES TO app_write;

COMMIT;
