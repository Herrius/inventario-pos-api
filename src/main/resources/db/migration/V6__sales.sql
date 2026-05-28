-- M4: ventas (POS) + líneas de venta.
--
-- DECISIONES DE DISEÑO:
--   * total NUMERIC(14,2): la suma puede crecer más que un precio unitario;
--     2 decimales por la moneda; CHECK >= 0.
--   * status: en B2 MVP toda venta nace PAGADA (POS contado); ANULADA queda
--     reservado para un flujo de devolución futuro. Validado por CHECK.
--   * precio_unitario en sale_items: SNAPSHOT del precio del producto al
--     momento de la venta. Si mañana el ADMIN cambia el precio del producto,
--     la venta de ayer no muta. Misma decisión que B1.
--   * subtotal NUMERIC(14,2): denormalizado por dos razones:
--       (a) los reportes (M5) leen sin recalcular,
--       (b) auditoría (si hay error de redondeo o promo aplicada en código,
--           queda registrado el subtotal real cobrado).
--   * FK ON DELETE RESTRICT: borrar un producto que ya tuvo ventas debe fallar
--     (referencia histórica).
--   * ALTER TABLE stock_movement: ahora que existe sales, agregamos la FK
--     que en V5 quedó como columna BIGINT NULL.

CREATE TABLE sales (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT         NOT NULL REFERENCES users(id),
    total       NUMERIC(14, 2) NOT NULL CHECK (total >= 0),
    status      VARCHAR(20)    NOT NULL CHECK (status IN ('PAGADA', 'ANULADA')),
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TABLE sale_items (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sale_id          BIGINT         NOT NULL REFERENCES sales(id) ON DELETE RESTRICT,
    product_id       BIGINT         NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    cantidad         INTEGER        NOT NULL CHECK (cantidad > 0),
    precio_unitario  NUMERIC(12, 2) NOT NULL CHECK (precio_unitario >= 0),
    subtotal         NUMERIC(14, 2) NOT NULL CHECK (subtotal >= 0)
);

-- Índices para reportes y consultas frecuentes:
CREATE INDEX idx_sales_created_at      ON sales(created_at DESC);
CREATE INDEX idx_sales_user_time       ON sales(user_id, created_at DESC);
CREATE INDEX idx_sale_items_sale       ON sale_items(sale_id);
CREATE INDEX idx_sale_items_product    ON sale_items(product_id);

-- Cerrar la trazabilidad: stock_movement.sale_id ahora tiene FK real.
ALTER TABLE stock_movement
    ADD CONSTRAINT fk_stock_movement_sale
    FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE RESTRICT;
