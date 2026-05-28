-- M3: log inmutable de movimientos de stock.
--
-- DISEÑO (el diferenciador #1 del proyecto vs un CRUD genérico):
--   * Append-only: este log es la VERDAD del stock. Product.stock_actual es una
--     proyección (cache). Toda mutación de stock_actual ocurre DENTRO de la
--     misma transacción que crea un StockMovement.
--   * Sin update/delete: para "corregir" un error se crea otro movimiento de
--     tipo AJUSTE_* — el log queda auditable.
--   * Sin @Version: append-only no tiene contención de updates.
--   * cantidad SIEMPRE positiva (CHECK). El signo del efecto lo determina el tipo:
--       ENTRADA          → +cantidad   (compra a proveedor)
--       SALIDA           → -cantidad   (venta — generada en M4)
--       AJUSTE_POSITIVO  → +cantidad   (conteo físico, devolución de cliente)
--       AJUSTE_NEGATIVO  → -cantidad   (merma, robo, daño)
--     Por qué cantidad positiva + tipo: que los reportes y queries lean el log
--     sin tener que conocer convenciones de signo. Filtros como "total entradas
--     del mes" o "total mermas" se hacen por tipo + suma directa.
--   * sale_id NULL ahora; en M4 (cuando exista la tabla sales) se agrega la FK
--     con ALTER TABLE. Permite trazar "este movimiento se generó por la venta X".

CREATE TABLE stock_movement (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id  BIGINT       NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    tipo        VARCHAR(20)  NOT NULL
                 CHECK (tipo IN ('ENTRADA', 'SALIDA', 'AJUSTE_POSITIVO', 'AJUSTE_NEGATIVO')),
    cantidad    INTEGER      NOT NULL CHECK (cantidad > 0),
    razon       TEXT,
    sale_id     BIGINT,
    user_id     BIGINT       NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Índices según los queries esperados:
--   * Log de un producto en un rango → (product_id, created_at)
--   * Reportes temporales globales → (created_at)
--   * Filtro por tipo (ENTRADA vs AJUSTE_NEGATIVO) → (tipo)
CREATE INDEX idx_stock_movement_product_time ON stock_movement(product_id, created_at DESC);
CREATE INDEX idx_stock_movement_created_at    ON stock_movement(created_at DESC);
CREATE INDEX idx_stock_movement_tipo          ON stock_movement(tipo);
