
-- Warehouse Picker Route Optimization Platform
-- Test data: warehouse layout
-- PostgreSQL

BEGIN;

DO $$
DECLARE
v_warehouse_id BIGINT;
BEGIN

    -- Prevent duplicate demo warehouses
    IF EXISTS (
        SELECT 1
        FROM public.warehouses
        WHERE name = 'Demo Warehouse'
    ) THEN
        RAISE EXCEPTION 'Demo Warehouse already exists';
END IF;

    -- 1. Create warehouse
INSERT INTO public.warehouses (name, description)
VALUES (
           'Demo Warehouse',
           'Test warehouse for route optimization'
       )
    RETURNING id INTO v_warehouse_id;

-- 2. Create warehouse nodes
INSERT INTO public.warehouse_nodes (
    warehouse_id,
    node_code,
    node_type,
    x_coordinate,
    y_coordinate
)
VALUES
    (v_warehouse_id, 'START', 'ENTRANCE', 0, 0),
    (v_warehouse_id, 'EXIT', 'EXIT', 20, 0),

    (v_warehouse_id, 'I1', 'INTERSECTION', 10, 30),
    (v_warehouse_id, 'I2', 'INTERSECTION', 10, 20),
    (v_warehouse_id, 'I3', 'INTERSECTION', 10, 10),
    (v_warehouse_id, 'I4', 'INTERSECTION', 10, 0),

    (v_warehouse_id, 'S1', 'STORAGE', 0, 30),
    (v_warehouse_id, 'S2', 'STORAGE', 20, 30),
    (v_warehouse_id, 'S3', 'STORAGE', 0, 20),
    (v_warehouse_id, 'S4', 'STORAGE', 20, 20),
    (v_warehouse_id, 'S5', 'STORAGE', 0, 10),
    (v_warehouse_id, 'S6', 'STORAGE', 20, 10);

-- 3. Create warehouse edges
INSERT INTO public.warehouse_edges (
    warehouse_id,
    from_node_id,
    to_node_id,
    distance_m,
    is_bidirectional
)
SELECT
    v_warehouse_id,
    n1.id,
    n2.id,
    e.distance_m,
    TRUE
FROM (
         VALUES
             ('START', 'I4', 10.00),
             ('I4', 'EXIT', 10.00),

             ('I4', 'I3', 10.00),
             ('I3', 'I2', 10.00),
             ('I2', 'I1', 10.00),

             ('START', 'S5', 10.00),
             ('S5', 'S3', 10.00),
             ('S3', 'S1', 10.00),

             ('EXIT', 'S6', 10.00),
             ('S6', 'S4', 10.00),
             ('S4', 'S2', 10.00),

             ('S1', 'I1', 10.00),
             ('I1', 'S2', 10.00),

             ('S3', 'I2', 10.00),
             ('I2', 'S4', 10.00),

             ('S5', 'I3', 10.00),
             ('I3', 'S6', 10.00)
     ) AS e(from_code, to_code, distance_m)

         JOIN public.warehouse_nodes n1
              ON n1.warehouse_id = v_warehouse_id
                  AND n1.node_code = e.from_code

         JOIN public.warehouse_nodes n2
              ON n2.warehouse_id = v_warehouse_id
                  AND n2.node_code = e.to_code;

END $$;

COMMIT;


-- 4. Create products and inventory

BEGIN;

DO $$
DECLARE
v_warehouse_id BIGINT;
BEGIN

SELECT id INTO STRICT v_warehouse_id
FROM public.warehouses
WHERE name = 'Demo Warehouse';

-- Create products
INSERT INTO public.products (
    sku,
    name,
    weight_kg
)
VALUES
    ('DEMO-001', 'LED Light Bulb', 0.150),
    ('DEMO-002', 'Ceramic Mug', 0.350),
    ('DEMO-003', 'Desk Lamp', 1.500),
    ('DEMO-004', 'Office Chair', 8.000),
    ('DEMO-005', 'Bookshelf', 15.000),
    ('DEMO-006', 'Storage Cabinet', 25.000);

-- Assign products to storage locations
INSERT INTO public.product_locations (
    product_id,
    node_id,
    quantity
)
SELECT
    p.id,
    n.id,
    s.quantity
FROM (
         VALUES
             ('DEMO-001', 'S1', 100),
             ('DEMO-002', 'S2', 80),
             ('DEMO-003', 'S3', 40),
             ('DEMO-004', 'S4', 20),
             ('DEMO-005', 'S5', 15),
             ('DEMO-006', 'S6', 10)
     ) AS s(sku, node_code, quantity)

         JOIN public.products p
              ON p.sku = s.sku

         JOIN public.warehouse_nodes n
              ON n.node_code = s.node_code
                  AND n.warehouse_id = v_warehouse_id;

END $$;

COMMIT;


-- 5. Create demo users

BEGIN;

INSERT INTO public.users (
    username,
    password_hash,
    full_name,
    role,
    is_active
)
VALUES
    (
        'demo_manager',
        '$2y$12$D/mWWoWFLLBFPH7.kEL0h.UfEIW9XOn3cZIFuAcv7usQRiZg1HBlS',
        'Alex Morgan',
        'MANAGER',
        TRUE
    ),
    (
        'demo_picker_1',
        '$2y$12$P9t3oPAGlDX4YQeZZQx66OsYAOnKdiblMqIRpvfXZPYjOzS3XlWAC',
        'Daniel Smith',
        'PICKER',
        TRUE
    ),
    (
        'demo_picker_2',
        '$2y$12$FrQClr14NORkyYF5lkdXgeQheBZ79kcYkPIfxnCtMEJWuJ9Hcb1nK',
        'Emma Wilson',
        'PICKER',
        TRUE
    );

COMMIT;


-- 6. Create picking tasks and task items

BEGIN;

DO $$
DECLARE
v_warehouse_id BIGINT;
    v_manager_id BIGINT;
    v_picker_1_id BIGINT;
    v_picker_2_id BIGINT;

    v_task_1_id BIGINT;
    v_task_2_id BIGINT;
    v_task_3_id BIGINT;
BEGIN

    -- Find warehouse and users
SELECT id INTO STRICT v_warehouse_id
FROM public.warehouses
WHERE name = 'Demo Warehouse';

SELECT id INTO STRICT v_manager_id
FROM public.users
WHERE username = 'demo_manager';

SELECT id INTO STRICT v_picker_1_id
FROM public.users
WHERE username = 'demo_picker_1';

SELECT id INTO STRICT v_picker_2_id
FROM public.users
WHERE username = 'demo_picker_2';

-- Task 1: New task
INSERT INTO public.picking_tasks (
    warehouse_id,
    created_by_id,
    status
)
VALUES (
           v_warehouse_id,
           v_manager_id,
           'NEW'
       )
    RETURNING id INTO v_task_1_id;

-- Task 2: Assigned task
INSERT INTO public.picking_tasks (
    warehouse_id,
    created_by_id,
    assigned_picker_id,
    status
)
VALUES (
           v_warehouse_id,
           v_manager_id,
           v_picker_1_id,
           'ASSIGNED'
       )
    RETURNING id INTO v_task_2_id;

-- Task 3: Task in progress
INSERT INTO public.picking_tasks (
    warehouse_id,
    created_by_id,
    assigned_picker_id,
    status,
    started_at
)
VALUES (
           v_warehouse_id,
           v_manager_id,
           v_picker_2_id,
           'IN_PROGRESS',
           NOW()
       )
    RETURNING id INTO v_task_3_id;

-- Add products to Task 1
INSERT INTO public.picking_task_items (
    task_id,
    product_id,
    required_quantity,
    collected_quantity
)
SELECT
    v_task_1_id,
    p.id,
    i.quantity,
    0
FROM (
         VALUES
             ('DEMO-001', 5),
             ('DEMO-002', 3),
             ('DEMO-003', 2)
     ) AS i(sku, quantity)
         JOIN public.products p ON p.sku = i.sku;

-- Add products to Task 2
INSERT INTO public.picking_task_items (
    task_id,
    product_id,
    required_quantity,
    collected_quantity
)
SELECT
    v_task_2_id,
    p.id,
    i.quantity,
    0
FROM (
         VALUES
             ('DEMO-001', 2),
             ('DEMO-004', 1),
             ('DEMO-006', 1)
     ) AS i(sku, quantity)
         JOIN public.products p ON p.sku = i.sku;

-- Add products to Task 3
INSERT INTO public.picking_task_items (
    task_id,
    product_id,
    required_quantity,
    collected_quantity
)
SELECT
    v_task_3_id,
    p.id,
    i.quantity,
    0
FROM (
         VALUES
             ('DEMO-002', 4),
             ('DEMO-005', 1)
     ) AS i(sku, quantity)
         JOIN public.products p ON p.sku = i.sku;

END $$;

COMMIT;


-- 7. Create demonstration routes and route stops

BEGIN;

DO $$
DECLARE
v_warehouse_id BIGINT;
    v_task_id BIGINT;
    v_route_id BIGINT;
    v_algorithm VARCHAR(30);
    v_path TEXT[];
    v_node_code TEXT;
    v_node_id BIGINT;
    v_previous_node_id BIGINT;
    v_product_id BIGINT;
    v_quantity INTEGER;
    v_stop_order INTEGER;
    v_distance NUMERIC(12,2);
    v_weight NUMERIC(12,3);
    v_carried_weight NUMERIC(12,3);
    v_total_distance NUMERIC(12,2);
    v_load_distance NUMERIC(16,3);
BEGIN

SELECT id INTO STRICT v_warehouse_id
FROM public.warehouses
WHERE name = 'Demo Warehouse';

-- Find Task 2 using its warehouse and picker
SELECT t.id INTO STRICT v_task_id
FROM public.picking_tasks t
    JOIN public.users u
ON u.id = t.assigned_picker_id
WHERE t.warehouse_id = v_warehouse_id
  AND u.username = 'demo_picker_1'
  AND t.status = 'ASSIGNED';

-- Create both demonstration routes
FOREACH v_algorithm IN ARRAY
        ARRAY['S_SHAPE', 'OPTIMIZED']
    LOOP

        IF v_algorithm = 'S_SHAPE' THEN
            v_path := ARRAY[
                'START', 'I4', 'I3',
                'S6', 'S4', 'S2',
                'I1', 'S1', 'S3',
                'S5', 'START', 'I4', 'EXIT'
            ];
ELSE
            v_path := ARRAY[
                'START', 'S5', 'S3',
                'S1', 'I1', 'S2',
                'S4', 'S6', 'EXIT'
            ];
END IF;

        -- Initialize route metrics
        v_previous_node_id := NULL;
        v_stop_order := 0;
        v_carried_weight := 0;
        v_total_distance := 0;
        v_load_distance := 0;

INSERT INTO public.picking_routes (
    task_id,
    algorithm,
    total_distance_m,
    load_distance_kg_m,
    calculation_time_ms
)
VALUES (
           v_task_id,
           v_algorithm,
           0,
           0,
           0
       )
    RETURNING id INTO v_route_id;

-- Create each stop in the route
FOREACH v_node_code IN ARRAY v_path
        LOOP

            v_stop_order := v_stop_order + 1;

SELECT id INTO STRICT v_node_id
FROM public.warehouse_nodes
WHERE warehouse_id = v_warehouse_id
  AND node_code = v_node_code;

-- Calculate distance from previous node
IF v_previous_node_id IS NULL THEN
                v_distance := 0;
ELSE
SELECT distance_m INTO STRICT v_distance
FROM public.warehouse_edges
WHERE warehouse_id = v_warehouse_id
  AND (
    (
    from_node_id = v_previous_node_id
  AND to_node_id = v_node_id
    )
   OR
    (
    from_node_id = v_node_id
  AND to_node_id = v_previous_node_id
  AND is_bidirectional = TRUE
    )
    );
END IF;

            -- Load carried during the current segment
            v_total_distance :=
                v_total_distance + v_distance;

            v_load_distance :=
                v_load_distance
                + v_carried_weight * v_distance;

            -- Navigation stops have no product
            v_product_id := NULL;
            v_quantity := NULL;
            v_weight := 0;

            -- Collect the required product at this node
SELECT
    p.id,
    i.required_quantity,
    p.weight_kg * i.required_quantity
INTO
    v_product_id,
    v_quantity,
    v_weight
FROM public.picking_task_items i
         JOIN public.products p
              ON p.id = i.product_id
         JOIN public.product_locations pl
              ON pl.product_id = p.id
WHERE i.task_id = v_task_id
  AND pl.node_id = v_node_id;

v_carried_weight :=
                v_carried_weight + COALESCE(v_weight, 0);

INSERT INTO public.route_stops (
    route_id,
    node_id,
    stop_order,
    product_id,
    quantity,
    distance_from_previous_m,
    carried_weight_after_kg
)
VALUES (
           v_route_id,
           v_node_id,
           v_stop_order,
           v_product_id,
           v_quantity,
           v_distance,
           v_carried_weight
       );

v_previous_node_id := v_node_id;

END LOOP;

        -- Save calculated route metrics
UPDATE public.picking_routes
SET total_distance_m = v_total_distance,
    load_distance_kg_m = v_load_distance
WHERE id = v_route_id;

END LOOP;

END $$;

COMMIT;


