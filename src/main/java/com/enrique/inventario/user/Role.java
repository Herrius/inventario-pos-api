package com.enrique.inventario.user;

/**
 * Roles de autorización. Se guardan como texto (EnumType.STRING) en la columna role.
 *
 * - ADMIN: configura el catálogo (productos/categorías), registra entradas de stock,
 *   hace ajustes (mermas, conteos físicos), ve todos los reportes.
 * - CAJERO: registra ventas en el POS y ve el catálogo. NO modifica catálogo
 *   ni hace ajustes de stock.
 */
public enum Role {
    CAJERO,
    ADMIN
}
