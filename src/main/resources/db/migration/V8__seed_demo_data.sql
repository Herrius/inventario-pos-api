-- M5.5: datos de demo. Es una migración versionada (no repetible) para que
-- el demo siga el principio de Flyway: el esquema arranca conocido.
-- Si quieres "resetear" el demo, drop DB + redespliega.
--
-- USUARIOS:
--   admin@demo.com  / admin123  (ADMIN)
--   cajero@demo.com / cajero123 (CAJERO)
--
-- DATA: 5 categorías, 30 productos, ~720 ventas distribuidas en 90 días
-- con variación temporal real (viernes/sábado más ventas, picos diarios).
-- Los stock_movement de SALIDA cierran la trazabilidad de cada venta.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Usuarios de demo: el hash se genera con BCrypt cost 10 vía pgcrypto.
-- Spring Security BCryptPasswordEncoder valida ambos prefijos $2a$ y $2b$.
INSERT INTO users (email, password, role, created_at) VALUES
  ('admin@demo.com',  crypt('admin123',  gen_salt('bf', 10)), 'ADMIN',  NOW() - INTERVAL '120 days'),
  ('cajero@demo.com', crypt('cajero123', gen_salt('bf', 10)), 'CAJERO', NOW() - INTERVAL '120 days');

INSERT INTO categories (nombre, created_at) VALUES
  ('Bebidas',   NOW() - INTERVAL '120 days'),
  ('Snacks',    NOW() - INTERVAL '120 days'),
  ('Lacteos',   NOW() - INTERVAL '120 days'),
  ('Aseo',      NOW() - INTERVAL '120 days'),
  ('Abarrotes', NOW() - INTERVAL '120 days');

-- 30 productos repartidos en las 5 categorías. Precios en soles (PEN) realistas.
INSERT INTO products (sku, nombre, precio, category_id, stock_actual, min_stock, version, created_at) VALUES
  -- Bebidas (1)
  ('COCA-500',   'Coca Cola 500ml',         3.50, 1, 0, 20, 0, NOW() - INTERVAL '110 days'),
  ('COCA-1L',    'Coca Cola 1L',            5.50, 1, 0, 15, 0, NOW() - INTERVAL '110 days'),
  ('INKA-500',   'Inca Kola 500ml',         3.50, 1, 0, 20, 0, NOW() - INTERVAL '110 days'),
  ('INKA-1L',    'Inca Kola 1L',            5.50, 1, 0, 15, 0, NOW() - INTERVAL '110 days'),
  ('AGUA-625',   'Agua San Luis 625ml',     1.50, 1, 0, 30, 0, NOW() - INTERVAL '110 days'),
  ('RED-355',    'Red Bull 355ml',          7.00, 1, 0, 12, 0, NOW() - INTERVAL '110 days'),
  ('FRUG-296',   'Frugos 296ml',            2.50, 1, 0, 25, 0, NOW() - INTERVAL '110 days'),
  ('SPRT-500',   'Sporade 500ml',           3.00, 1, 0, 18, 0, NOW() - INTERVAL '110 days'),
  -- Snacks (2)
  ('LAYS-100',   'Lays Clásicas 100g',      4.20, 2, 0, 15, 0, NOW() - INTERVAL '110 days'),
  ('DORI-150',   'Doritos 150g',            5.50, 2, 0, 12, 0, NOW() - INTERVAL '110 days'),
  ('CHIZ-130',   'Chizitos 130g',           3.80, 2, 0, 15, 0, NOW() - INTERVAL '110 days'),
  ('PIQU-90',    'Piqueo Snax 90g',         3.00, 2, 0, 18, 0, NOW() - INTERVAL '110 days'),
  ('CHEE-150',   'Cheese Tris 150g',        4.50, 2, 0, 12, 0, NOW() - INTERVAL '110 days'),
  ('OREO-118',   'Oreo Original 118g',      4.20, 2, 0, 14, 0, NOW() - INTERVAL '110 days'),
  -- Lacteos (3)
  ('LECH-1L',    'Leche Gloria 1L',         5.20, 3, 0, 20, 0, NOW() - INTERVAL '110 days'),
  ('YOPL-200',   'Yogurt Yoplait 200g',     3.50, 3, 0, 18, 0, NOW() - INTERVAL '110 days'),
  ('QUES-200',   'Queso Fresco 200g',       9.50, 3, 0, 10, 0, NOW() - INTERVAL '110 days'),
  ('MANT-200',   'Mantequilla Laive 200g', 12.50, 3, 0, 8,  0, NOW() - INTERVAL '110 days'),
  -- Aseo (4)
  ('JAB-PRO',    'Jabón Protex 90g',        3.00, 4, 0, 20, 0, NOW() - INTERVAL '110 days'),
  ('SHA-PAN',    'Shampoo Pantene 400ml',  15.90, 4, 0, 10, 0, NOW() - INTERVAL '110 days'),
  ('PAS-COLG',   'Pasta Colgate 100ml',     6.50, 4, 0, 15, 0, NOW() - INTERVAL '110 days'),
  ('PAP-HIG',    'Papel Higiénico x4',      6.00, 4, 0, 18, 0, NOW() - INTERVAL '110 days'),
  ('DET-ARI',    'Detergente Ariel 360g',  10.50, 4, 0, 12, 0, NOW() - INTERVAL '110 days'),
  -- Abarrotes (5)
  ('ARR-COST',   'Arroz Costeño 750g',      5.50, 5, 0, 25, 0, NOW() - INTERVAL '110 days'),
  ('ACE-PRIM',   'Aceite Primor 1L',       11.50, 5, 0, 12, 0, NOW() - INTERVAL '110 days'),
  ('AZU-PAR',    'Azúcar Paramonga 1kg',    4.50, 5, 0, 20, 0, NOW() - INTERVAL '110 days'),
  ('FID-DON',    'Fideos Don Vittorio',     3.20, 5, 0, 18, 0, NOW() - INTERVAL '110 days'),
  ('ATN-FLOR',   'Atún Florida 170g',       5.80, 5, 0, 15, 0, NOW() - INTERVAL '110 days'),
  ('SAL-EMS',    'Sal Emsal 1kg',           2.50, 5, 0, 22, 0, NOW() - INTERVAL '110 days'),
  ('LECH-COND',  'Leche Condensada 397g',   7.50, 5, 0, 10, 0, NOW() - INTERVAL '110 days');

-- ENTRADAS iniciales: 500 unidades por producto (suficiente para 90 días de ventas).
-- Las hacemos hace 100 días para que estén "antes" del rango de ventas.
WITH admin_user AS (SELECT id FROM users WHERE email = 'admin@demo.com')
INSERT INTO stock_movement (product_id, tipo, cantidad, razon, sale_id, user_id, created_at)
SELECT p.id, 'ENTRADA', 500, 'Compra inicial a proveedor', NULL, (SELECT id FROM admin_user),
       NOW() - INTERVAL '100 days'
FROM products p;

UPDATE products SET stock_actual = 500;  -- proyección sincronizada con las entradas

-- ============================================================================
-- GENERACIÓN DE 90 DÍAS DE VENTAS CON VARIACIÓN TEMPORAL
-- ============================================================================
-- Patrón:
--   * Por día: 6-12 ventas base, +50% en viernes/sábado (DOW 5-6).
--   * Por venta: 1-4 ítems, cada uno con 1-3 unidades.
--   * Hora distribuida entre 09:00-21:00 con campana centrada al mediodía
--     (más realista que uniform; aprox vía promedio de dos randoms).
--   * Producto aleatorio del catálogo (uniforme; un modelo más fino pondría
--     peso a Bebidas/Snacks vs Aseo, pero suficiente para el demo).
-- ============================================================================
DO $$
DECLARE
    cajero_id BIGINT;
    sales_for_day INT;
    sale_id BIGINT;
    sale_at TIMESTAMPTZ;
    item_count INT;
    sale_total NUMERIC(14,2);
    prod RECORD;
    qty INT;
    snapshot_price NUMERIC(12,2);
    subt NUMERIC(14,2);
    d INT;
    s INT;
    i INT;
    dow INT;  -- día de la semana
BEGIN
    SELECT id INTO cajero_id FROM users WHERE email = 'cajero@demo.com';

    FOR d IN 0..89 LOOP
        dow := EXTRACT(DOW FROM (CURRENT_DATE - d))::INT;  -- 0=domingo .. 6=sábado
        -- Base 6-12 ventas; viernes (5) y sábado (6) +50%.
        sales_for_day := 6 + (random() * 6)::INT;
        IF dow IN (5, 6) THEN
            sales_for_day := (sales_for_day * 1.5)::INT;
        END IF;

        FOR s IN 1..sales_for_day LOOP
            -- Hora: campana suave ((rand+rand)/2 ≈ 0.5 ± 0.25), escalada a 09-21.
            sale_at := (CURRENT_DATE - d)::TIMESTAMPTZ
                       + INTERVAL '9 hours'
                       + ((random() + random()) / 2 * INTERVAL '12 hours');

            INSERT INTO sales (user_id, total, status, created_at)
            VALUES (cajero_id, 0, 'PAGADA', sale_at)
            RETURNING id INTO sale_id;

            sale_total := 0;
            item_count := 1 + (random() * 3)::INT;  -- 1..4

            -- IDs distintos por venta: usamos un WITH RECURSIVE ad hoc vía
            -- iterar y filtrar con un set de IDs ya añadidos (en plpgsql
            -- usamos un arreglo).
            FOR i IN 1..item_count LOOP
                -- Pickear producto random (puede repetir entre ventas distintas).
                SELECT id, precio INTO prod
                FROM products
                ORDER BY random()
                LIMIT 1;

                qty := 1 + (random() * 2)::INT;  -- 1..3 unidades
                snapshot_price := prod.precio;
                subt := snapshot_price * qty;

                INSERT INTO sale_items (sale_id, product_id, cantidad, precio_unitario, subtotal)
                VALUES (sale_id, prod.id, qty, snapshot_price, subt);

                -- Movimiento SALIDA correspondiente.
                INSERT INTO stock_movement (product_id, tipo, cantidad, razon, sale_id, user_id, created_at)
                VALUES (prod.id, 'SALIDA', qty, 'Venta #' || sale_id, sale_id, cajero_id, sale_at);

                -- Decrementar la proyección (CHECK >= 0 fallaría si lo agotamos;
                -- iniciamos con 500 cada uno, suficiente para 90 días de ventas).
                UPDATE products SET stock_actual = stock_actual - qty WHERE id = prod.id;

                sale_total := sale_total + subt;
            END LOOP;

            UPDATE sales SET total = sale_total WHERE id = sale_id;
        END LOOP;
    END LOOP;
END $$;

-- Algunos ajustes negativos para que el reporte de movimientos no sea solo
-- ENTRADAS y SALIDAS limpias (refleja la realidad de un POS: hay merma).
WITH admin_user AS (SELECT id FROM users WHERE email = 'admin@demo.com')
INSERT INTO stock_movement (product_id, tipo, cantidad, razon, sale_id, user_id, created_at)
VALUES
  (9,  'AJUSTE_NEGATIVO', 3,  'Botellas rotas en estante',  NULL, (SELECT id FROM admin_user), NOW() - INTERVAL '45 days'),
  (15, 'AJUSTE_NEGATIVO', 5,  'Vencimiento',                NULL, (SELECT id FROM admin_user), NOW() - INTERVAL '30 days'),
  (22, 'AJUSTE_POSITIVO', 4,  'Sobrante encontrado en conteo físico', NULL, (SELECT id FROM admin_user), NOW() - INTERVAL '15 days');

UPDATE products SET stock_actual = stock_actual - 3 WHERE id = 9;
UPDATE products SET stock_actual = stock_actual - 5 WHERE id = 15;
UPDATE products SET stock_actual = stock_actual + 4 WHERE id = 22;
