-- V5: Create picking routes and route stops
-- Warehouse Picker Route Optimization Platform

CREATE TABLE public.picking_routes
(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id             BIGINT         NOT NULL,
    algorithm           VARCHAR(30)    NOT NULL,
    total_distance_m    NUMERIC(12, 2) NOT NULL,
    load_distance_kg_m  NUMERIC(16, 3) NOT NULL,
    calculation_time_ms INTEGER        NOT NULL,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_route_task
        FOREIGN KEY (task_id)
            REFERENCES public.picking_tasks (id),

    CONSTRAINT chk_route_algorithm
        CHECK (algorithm IN ('S_SHAPE', 'OPTIMIZED')),

    CONSTRAINT chk_route_distance
        CHECK (total_distance_m >= 0),

    CONSTRAINT chk_route_load_distance
        CHECK (load_distance_kg_m >= 0),

    CONSTRAINT chk_route_calculation_time
        CHECK (calculation_time_ms >= 0)
);

CREATE TABLE public.route_stops
(
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    route_id                 BIGINT         NOT NULL,
    node_id                  BIGINT         NOT NULL,
    stop_order               INTEGER        NOT NULL,
    product_id               BIGINT,
    quantity                 INTEGER,
    distance_from_previous_m NUMERIC(12, 2) NOT NULL,
    carried_weight_after_kg  NUMERIC(12, 3) NOT NULL,

    CONSTRAINT fk_stop_route
        FOREIGN KEY (route_id)
            REFERENCES public.picking_routes (id),

    CONSTRAINT fk_stop_node
        FOREIGN KEY (node_id)
            REFERENCES public.warehouse_nodes (id),

    CONSTRAINT fk_stop_product
        FOREIGN KEY (product_id)
            REFERENCES public.products (id),

    CONSTRAINT uq_route_stop_order
        UNIQUE (route_id, stop_order),

    CONSTRAINT chk_stop_order
        CHECK (stop_order >= 1),

    CONSTRAINT chk_stop_quantity
        CHECK (
            (product_id IS NULL AND quantity IS NULL)
                OR
            (product_id IS NOT NULL
                AND quantity IS NOT NULL
                AND quantity > 0)
            ),

    CONSTRAINT chk_stop_distance
        CHECK (distance_from_previous_m >= 0),

    CONSTRAINT chk_carried_weight
        CHECK (carried_weight_after_kg >= 0)
);
