
-- Transaction test: atomic creation of a picking task

DO $$
DECLARE
v_task_id BIGINT;
    v_warehouse_id BIGINT;
    v_manager_id BIGINT;
    v_product_id BIGINT;
BEGIN
SELECT id INTO STRICT v_warehouse_id
FROM public.warehouses
WHERE name = 'Demo Warehouse';

SELECT id INTO STRICT v_manager_id
FROM public.users
WHERE username = 'demo_manager';

SELECT id INTO STRICT v_product_id
FROM public.products
WHERE sku = 'DEMO-001';

BEGIN
        -- Step 1: Create a picking task

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
    RETURNING id INTO v_task_id;

-- Step 2: Add a valid task item

INSERT INTO public.picking_task_items (
    task_id,
    product_id,
    required_quantity
)
VALUES (
           v_task_id,
           v_product_id,
           5
       );

-- Step 3: Add an invalid item

INSERT INTO public.picking_task_items (
    task_id,
    product_id,
    required_quantity
)
VALUES (
           v_task_id,
           v_product_id,
           -10
       );

RAISE EXCEPTION
            'TEST FAILED: Invalid quantity was accepted';

EXCEPTION
        WHEN check_violation THEN
            RAISE NOTICE
                'Invalid item rejected. Changes rolled back.';
END;

    -- Step 4: Verify that the task was rolled back

    IF EXISTS (
        SELECT 1
        FROM public.picking_tasks
        WHERE id = v_task_id
    ) THEN
        RAISE EXCEPTION
            'TEST FAILED: Picking task was not rolled back';
END IF;

    -- Step 5: Verify that the valid item was rolled back

    IF EXISTS (
        SELECT 1
        FROM public.picking_task_items
        WHERE task_id = v_task_id
    ) THEN
        RAISE EXCEPTION
            'TEST FAILED: Task items were not rolled back';
END IF;

    RAISE NOTICE
        'TEST PASSED: Task and items rolled back successfully';
END $$;
