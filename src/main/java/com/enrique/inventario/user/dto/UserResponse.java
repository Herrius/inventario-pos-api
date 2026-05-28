package com.enrique.inventario.user.dto;

import com.enrique.inventario.user.Role;
import java.time.OffsetDateTime;

/**
 * Lo que la API DEVUELVE de un usuario. Nota lo que NO está: el password.
 * Por eso usamos un DTO y no exponemos la entidad User directamente.
 */
public record UserResponse(Long id, String email, Role role, OffsetDateTime createdAt) {
}
