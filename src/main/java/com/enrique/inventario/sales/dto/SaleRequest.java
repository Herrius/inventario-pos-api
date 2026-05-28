package com.enrique.inventario.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaleRequest(
        @NotEmpty(message = "una venta debe tener al menos 1 ítem")
        @Size(max = 100, message = "máximo 100 ítems por venta")
        @Valid
        List<SaleItemRequest> items
) {}
