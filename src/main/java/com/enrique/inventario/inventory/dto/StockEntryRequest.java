package com.enrique.inventario.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Registro de una entrada de stock (compra a proveedor, ingreso inicial). */
public record StockEntryRequest(
        @NotNull
        Long productId,

        @NotNull
        @Min(value = 1, message = "cantidad debe ser >= 1")
        Integer cantidad,

        @Size(max = 500)
        String razon
) {}
