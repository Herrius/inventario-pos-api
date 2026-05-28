# Query plans de los reportes (M5)

Este documento recoge la salida de `EXPLAIN ANALYZE` para cada query de reporte,
confirmando que los índices diseñados se usan como se esperaba.

## Índices relevantes

- `idx_sales_status_time` (V7): `(status, created_at DESC)` — cubre los WHERE
  típicos `status='PAGADA' AND created_at IN rango`.
- `idx_sales_created_at` (V6): backup para queries sin filtro de status.
- `idx_sale_items_sale` (V6): join sale_items ↔ sales.
- `idx_sale_items_product` (V6): aggregaciones por producto.

## Query 1 — Resumen del día

**SQL:**
```sql
SELECT COALESCE(COUNT(*), 0),
       COALESCE(SUM(total), 0)::numeric(14,2),
       COALESCE(AVG(total), 0)::numeric(14,2)
FROM sales
WHERE status = 'PAGADA'
  AND created_at >= $1
  AND created_at <  $2;
```

**Plan:**
```
Aggregate  (cost=8.19..8.21 rows=1 width=44)
            (actual time=0.015..0.016 rows=1 loops=1)
  -> Index Scan using idx_sales_status_time on sales
        (cost=0.16..8.18 rows=1 width=18)
        (actual time=0.008..0.009 rows=5 loops=1)
     Index Cond: (((status)::text = 'PAGADA'::text)
                  AND (created_at >= ...)
                  AND (created_at <  ...))
Execution Time: 0.038 ms
```

✅ El `Index Scan using idx_sales_status_time` confirma que el índice compuesto
es el camino elegido — un solo recorrido del árbol B-tree filtra status + rango
temporal.

## Query 2 — Ventas agregadas por día (rango)

**SQL:**
```sql
SELECT date_trunc('day', created_at)::date AS dia,
       COUNT(*),
       SUM(total)::numeric(14,2)
FROM sales
WHERE status='PAGADA' AND created_at >= $1 AND created_at < $2
GROUP BY dia
ORDER BY dia;
```

**Plan:**
```
GroupAggregate
  Group Key: ((date_trunc('day', created_at))::date)
  -> Sort  Sort Key: ((date_trunc('day', created_at))::date)
     -> Index Scan using idx_sales_status_time on sales
Execution Time: 0.055 ms
```

✅ Mismo Index Scan; el Sort intermedio (necesario para el GroupAggregate) cabe
en memoria. Para datasets grandes, un `BRIN(created_at)` adicional sería opción.

## Query 3 — Top productos por unidades vendidas

**SQL:**
```sql
SELECT p.id, p.sku, p.nombre,
       SUM(si.cantidad) AS unidades,
       SUM(si.subtotal) AS revenue
FROM sale_items si
JOIN sales s    ON s.id = si.sale_id
JOIN products p ON p.id = si.product_id
WHERE s.status='PAGADA' AND s.created_at >= $1 AND s.created_at < $2
GROUP BY p.id, p.sku, p.nombre
ORDER BY unidades DESC
LIMIT $3;
```

**Plan:**
```
Limit
  -> Sort  Sort Key: (sum(si.cantidad)) DESC
     -> GroupAggregate  Group Key: p.id
        -> Sort  Sort Key: p.id
           -> Nested Loop
              -> Nested Loop
                 -> Index Scan using idx_sales_status_time on sales s
                    Index Cond: (status='PAGADA' AND created_at >= ...)
                 -> Bitmap Heap Scan on sale_items si
                    -> Bitmap Index Scan on idx_sale_items_sale
                       Index Cond: (sale_id = s.id)
              -> Index Scan using products_pkey on products p
                 Index Cond: (id = si.product_id)
Execution Time: 0.100 ms
```

✅ Los tres índices del modelo trabajan:
- `idx_sales_status_time` filtra ventas pagadas en rango.
- `idx_sale_items_sale` junta sale_items por venta.
- `products_pkey` resuelve cada producto en la join final.

## Lo que se rechazó (criterio)

- **Trigram (`pg_trgm`) en sales.created_at**: innecesario para rangos puros.
- **Materialized view de ventas-por-día**: el query agrega < 0.1 ms; sumar
  refresh overhead sería penalizar escrituras sin ganar lecturas reales.
- **Particionado por mes**: el dataset esperado (un MYPE) no lo justifica;
  agregar particiones aumenta complejidad operacional sin beneficio medible.
