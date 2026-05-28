package com.enrique.inventario.inventory;

/**
 * Tipo de movimiento de stock. Define el signo del efecto sobre {@code Product.stock_actual}:
 *  - ENTRADA, AJUSTE_POSITIVO        → suma {@code +cantidad}
 *  - SALIDA, AJUSTE_NEGATIVO         → resta {@code -cantidad}
 *
 * La columna {@code cantidad} en la BD es SIEMPRE positiva (CHECK constraint).
 * Que el signo viva en el tipo y no en la cantidad hace los reportes triviales
 * ("sum(cantidad) where tipo='AJUSTE_NEGATIVO'" = mermas totales).
 */
public enum StockMovementType {
    ENTRADA(1),
    SALIDA(-1),
    AJUSTE_POSITIVO(1),
    AJUSTE_NEGATIVO(-1);

    private final int sign;

    StockMovementType(int sign) {
        this.sign = sign;
    }

    /** Devuelve +1 o -1 según el efecto sobre {@code stock_actual}. */
    public int sign() {
        return sign;
    }
}
