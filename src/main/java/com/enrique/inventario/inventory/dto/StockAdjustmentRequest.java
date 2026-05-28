package com.enrique.inventario.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ajuste manual de stock. La dirección la decide el ADMIN según el motivo:
 *   - POSITIVO: encontró más unidades de las que el sistema decía (conteo físico).
 *   - NEGATIVO: merma, robo, daño, vencimiento.
 */
public record StockAdjustmentRequest(
        @NotNull
        Long productId,

        @NotNull
        Direction direccion,

        @NotNull
        @Min(value = 1, message = "cantidad debe ser >= 1")
        Integer cantidad,

        @Size(max = 500)
        String razon
) {
    public enum Direction { POSITIVO, NEGATIVO }
}
