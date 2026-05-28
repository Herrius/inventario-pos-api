-- M2: productos del catálogo.
-- DECISIONES DE DISEÑO:
--   * sku UNIQUE: el SKU es identificador de negocio (lo usa el cajero/dueño);
--     id es el técnico. Ambos sirven, sku es human-readable.
--   * precio NUMERIC(12,2): dinero NUNCA en double/float (errores de redondeo).
--     12,2 ≈ hasta 9.999.999.999,99 — más que suficiente para un MYPE.
--   * stock_actual: PROYECCIÓN. La verdad será StockMovement (M3).
--     Aquí lo mantenemos como cache para queries rápidas y para validar disponibilidad
--     antes de descontar en una venta. Se actualiza DENTRO DE LA MISMA transacción
--     que crea el movimiento (M3/M4) con @Version anti-sobreventa.
--   * min_stock: umbral para alerta "bajo mínimo" (reporte M5).
--   * version: optimistic locking de JPA. Ver M4.
--   * FK con ON DELETE RESTRICT: no se puede borrar una categoría si tiene productos
--     (decisión: errar y obligar a re-categorizar antes que dejar productos huérfanos).

CREATE TABLE products (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku           VARCHAR(50)    NOT NULL UNIQUE,
    nombre        VARCHAR(200)   NOT NULL,
    precio        NUMERIC(12, 2) NOT NULL CHECK (precio >= 0),
    category_id   BIGINT         NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    stock_actual  INTEGER        NOT NULL DEFAULT 0 CHECK (stock_actual >= 0),
    min_stock     INTEGER        NOT NULL DEFAULT 0 CHECK (min_stock >= 0),
    version       BIGINT         NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Índices: el más frecuente será filtrar productos por categoría y buscar por nombre.
-- (sku ya tiene índice único implícito por la constraint UNIQUE.)
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_nombre_trgm ON products(nombre);  -- M5 lo reemplazará por trgm si hace falta búsqueda fuzzy.
