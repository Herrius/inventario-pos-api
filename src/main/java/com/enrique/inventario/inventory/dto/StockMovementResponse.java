package com.enrique.inventario.inventory.dto;

import com.enrique.inventario.inventory.StockMovementType;
import java.time.OffsetDateTime;

public record StockMovementResponse(
        Long id,
        Long productId,
        String productSku,
        String productNombre,
        StockMovementType tipo,
        Integer cantidad,
        int deltaConSigno,
        String razon,
        Long saleId,
        String userEmail,
        OffsetDateTime createdAt
) {}
