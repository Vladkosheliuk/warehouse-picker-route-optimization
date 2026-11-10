-- V1: Create warehouse structure
-- Warehouse Picker Route Optimization Platform

CREATE TABLE public.warehouses
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE public.warehouse_nodes
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    warehouse_id BIGINT         NOT NULL,
    node_code    VARCHAR(50)    NOT NULL,
    node_type    VARCHAR(30)    NOT NULL,
    x_coordinate NUMERIC(10, 2) NOT NULL,
    y_coordinate NUMERIC(10, 2) NOT NULL,

    CONSTRAINT fk_node_warehouse
        FOREIGN KEY (warehouse_id)
            REFERENCES public.warehouses (id),

    CONSTRAINT uq_warehouse_node
        UNIQUE (warehouse_id, node_code),

    CONSTRAINT uq_node_id_warehouse
        UNIQUE (id, warehouse_id),

    CONSTRAINT chk_node_type
        CHECK (node_type IN (
                             'ENTRANCE',
                             'EXIT',
                             'INTERSECTION',
                             'STORAGE'
            ))
);

CREATE TABLE public.warehouse_edges
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    warehouse_id     BIGINT         NOT NULL,
    from_node_id     BIGINT         NOT NULL,
    to_node_id       BIGINT         NOT NULL,
    distance_m       NUMERIC(10, 2) NOT NULL,
    is_bidirectional BOOLEAN        NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_edge_warehouse
        FOREIGN KEY (warehouse_id)
            REFERENCES public.warehouses (id),

    CONSTRAINT fk_edge_from_node
        FOREIGN KEY (from_node_id, warehouse_id)
            REFERENCES public.warehouse_nodes (id, warehouse_id),

    CONSTRAINT fk_edge_to_node
        FOREIGN KEY (to_node_id, warehouse_id)
            REFERENCES public.warehouse_nodes (id, warehouse_id),

    CONSTRAINT uq_warehouse_edge
        UNIQUE (
                warehouse_id,
                from_node_id,
                to_node_id
            ),

    CONSTRAINT chk_edge_distance
        CHECK (distance_m > 0),

    CONSTRAINT chk_different_nodes
        CHECK (from_node_id <> to_node_id)
);
