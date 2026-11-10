-- V2: Create products and product locations
-- Warehouse Picker Route Optimization Platform

CREATE TABLE public.products
(
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku       VARCHAR(50)    NOT NULL,
    name      VARCHAR(200)   NOT NULL,
    weight_kg NUMERIC(10, 3) NOT NULL,

    CONSTRAINT uq_product_sku
        UNIQUE (sku),

    CONSTRAINT chk_product_weight
        CHECK (weight_kg > 0)
);

CREATE TABLE public.product_locations
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT  NOT NULL,
    node_id    BIGINT  NOT NULL,
    quantity   INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_location_product
        FOREIGN KEY (product_id)
            REFERENCES public.products (id),

    CONSTRAINT fk_location_node
        FOREIGN KEY (node_id)
            REFERENCES public.warehouse_nodes (id),

    CONSTRAINT uq_product_location
        UNIQUE (product_id, node_id),

    CONSTRAINT chk_location_quantity
        CHECK (quantity >= 0)
);
