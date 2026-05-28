package com.enrique.inventario.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Body para crear o actualizar un producto. NOTA: no incluye {@code stockActual}
 * a propósito — el stock se modifica únicamente vía {@code StockMovement} (M3),
 * jamás mutado directo desde el endpoint del catálogo.
 */
public record ProductRequest(
        @NotBlank
        @Size(min = 1, max = 50)
        String sku,

        @NotBlank
        @Size(min = 2, max = 200)
        String nombre,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = true)
        @Digits(integer = 10, fraction = 2)
        BigDecimal precio,

        @NotNull
        Long categoryId,

        @Min(0)
        Integer minStock
) {}
