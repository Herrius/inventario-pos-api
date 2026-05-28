package com.enrique.inventario.sales.dto;

import java.math.BigDecimal;

public record SaleItemResponse(
        Long id,
        Long productId,
        String productSku,
        String productNombre,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {}
