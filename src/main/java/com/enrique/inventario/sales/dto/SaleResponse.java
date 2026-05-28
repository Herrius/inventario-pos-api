package com.enrique.inventario.sales.dto;

import com.enrique.inventario.sales.SaleStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record SaleResponse(
        Long id,
        String userEmail,
        BigDecimal total,
        SaleStatus status,
        OffsetDateTime createdAt,
        List<SaleItemResponse> items
) {}
