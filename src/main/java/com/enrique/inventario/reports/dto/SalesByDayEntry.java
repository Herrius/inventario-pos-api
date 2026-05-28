package com.enrique.inventario.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesByDayEntry(
        LocalDate dia,
        long totalVentas,
        BigDecimal revenue
) {}
