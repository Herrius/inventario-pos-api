package com.enrique.inventario.sales.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleItemRequest(
        @NotNull Long productId,
        @NotNull @Min(value = 1, message = "cantidad debe ser >= 1") Integer cantidad
) {}
