package com.enrique.inventario.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada del registro. La validación vive en el DTO (borde de la API),
 * así la lógica de negocio nunca recibe datos inválidos.
 * max=72 porque BCrypt solo considera los primeros 72 bytes del password.
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
