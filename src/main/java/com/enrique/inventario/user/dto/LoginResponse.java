package com.enrique.inventario.user.dto;

/** Lo que devuelve el login: el token de acceso y cuánto dura. */
public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
