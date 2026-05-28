package com.enrique.inventario.catalog.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProductResponse(
        Long id,
        String sku,
        String nombre,
        BigDecimal precio,
        Long categoryId,
        String categoryNombre,
        Integer stockActual,
        Integer minStock,
        OffsetDateTime createdAt
) {}
