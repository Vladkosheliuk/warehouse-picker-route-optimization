-- V4: Create picking tasks and task items
-- Warehouse Picker Route Optimization Platform

CREATE TABLE public.picking_tasks
(
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    warehouse_id       BIGINT      NOT NULL,
    created_by_id      BIGINT      NOT NULL,
    assigned_picker_id BIGINT,
    status             VARCHAR(30) NOT NULL DEFAULT 'NEW',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at         TIMESTAMPTZ,
    completed_at       TIMESTAMPTZ,

    CONSTRAINT fk_task_warehouse
        FOREIGN KEY (warehouse_id)
            REFERENCES public.warehouses (id),

    CONSTRAINT fk_task_creator
        FOREIGN KEY (created_by_id)
            REFERENCES public.users (id),

    CONSTRAINT fk_task_picker
        FOREIGN KEY (assigned_picker_id)
            REFERENCES public.users (id),

    CONSTRAINT chk_task_status
        CHECK (status IN (
                          'NEW',
                          'ASSIGNED',
                          'IN_PROGRESS',
                          'COMPLETED',
                          'CANCELLED'
            )),

    CONSTRAINT chk_task_dates
        CHECK (
            (started_at IS NULL OR started_at >= created_at)
                AND
            (completed_at IS NULL OR
             (started_at IS NOT NULL AND completed_at >= started_at))
            )
);

CREATE TABLE public.picking_task_items
(
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id            BIGINT  NOT NULL,
    product_id         BIGINT  NOT NULL,
    required_quantity  INTEGER NOT NULL,
    collected_quantity INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_task_item_task
        FOREIGN KEY (task_id)
            REFERENCES public.picking_tasks (id),

    CONSTRAINT fk_task_item_product
        FOREIGN KEY (product_id)
            REFERENCES public.products (id),

    CONSTRAINT uq_task_product
        UNIQUE (task_id, product_id),

    CONSTRAINT chk_required_quantity
        CHECK (required_quantity > 0),

    CONSTRAINT chk_collected_quantity
        CHECK (
            collected_quantity >= 0
                AND collected_quantity <= required_quantity
            )
);
