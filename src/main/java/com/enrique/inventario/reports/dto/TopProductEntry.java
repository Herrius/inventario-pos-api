package com.enrique.inventario.reports.dto;

import java.math.BigDecimal;

public record TopProductEntry(
        Long productId,
        String sku,
        String nombre,
        long unidadesVendidas,
        BigDecimal revenue
) {}
