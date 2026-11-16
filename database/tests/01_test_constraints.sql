
-- Test 1: Product weight must be positive

BEGIN;

DO $$
BEGIN
BEGIN
INSERT INTO public.products (
    sku,
    name,
    weight_kg
)
VALUES (
           'TEST-NEGATIVE-WEIGHT',
           'Invalid Test Product',
           -5.000
       );

RAISE EXCEPTION
            'TEST FAILED: Negative weight was accepted';

EXCEPTION
        WHEN check_violation THEN
            RAISE NOTICE
                'TEST PASSED: Negative weight was rejected';
END;
END $$;

ROLLBACK;


-- Test 2: Product SKU must be unique

BEGIN;

DO $$
BEGIN
BEGIN
INSERT INTO public.products (sku, name, weight_kg)
VALUES ('DEMO-001', 'Duplicate Product', 1.000);

RAISE EXCEPTION
            'TEST FAILED: Duplicate SKU was accepted';

EXCEPTION
        WHEN unique_violation THEN
            RAISE NOTICE
                'TEST PASSED: Duplicate SKU was rejected';
END;
END $$;


-- Test 3: Product name cannot be NULL

DO $$
BEGIN
BEGIN
INSERT INTO public.products (sku, name, weight_kg)
VALUES ('TEST-NULL-NAME', NULL, 1.000);

RAISE EXCEPTION
            'TEST FAILED: NULL product name was accepted';

EXCEPTION
        WHEN not_null_violation THEN
            RAISE NOTICE
                'TEST PASSED: NULL product name was rejected';
END;
END $$;


-- Test 4: Foreign key integrity

DO $$
BEGIN
BEGIN
INSERT INTO public.product_locations (
    product_id,
    node_id,
    quantity
)
VALUES (
           -1,
           (SELECT id FROM public.warehouse_nodes LIMIT 1),
    10
    );

RAISE EXCEPTION
            'TEST FAILED: Invalid product reference was accepted';

EXCEPTION
        WHEN foreign_key_violation THEN
            RAISE NOTICE
                'TEST PASSED: Invalid product reference was rejected';
END;
END $$;


-- Test 5: Stock quantity cannot be negative

DO $$
BEGIN
BEGIN
UPDATE public.product_locations
SET quantity = -10
WHERE id = (
    SELECT id
    FROM public.product_locations
             LIMIT 1
    );

RAISE EXCEPTION
            'TEST FAILED: Negative stock quantity was accepted';

EXCEPTION
        WHEN check_violation THEN
            RAISE NOTICE
                'TEST PASSED: Negative stock quantity was rejected';
END;
END $$;

ROLLBACK;

