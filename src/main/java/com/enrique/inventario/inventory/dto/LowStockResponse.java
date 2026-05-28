package com.enrique.inventario.inventory.dto;

public record LowStockResponse(
        Long productId,
        String sku,
        String nombre,
        String categoryNombre,
        Integer stockActual,
        Integer minStock,
        int faltante
) {}
