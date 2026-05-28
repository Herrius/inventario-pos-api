package com.enrique.inventario.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesDailySummaryResponse(
        LocalDate date,
        long totalVentas,
        BigDecimal revenue,
        BigDecimal ticketPromedio
) {}
