package com.enrique.inventario.sales;

/**
 * Estado de una venta.
 *  - PAGADA: estado normal al registrarse en el POS (contado).
 *  - ANULADA: reservado para un futuro flujo de devolución (no usado en B2 MVP).
 */
public enum SaleStatus {
    PAGADA,
    ANULADA
}
