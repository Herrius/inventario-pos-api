package com.enrique.inventario.common;

/**
 * Forma única de error de la API. errorCode es estable y máquina-legible;
 * message es para humanos; requestId ayuda a rastrear en logs; details es
 * opcional (p. ej. errores por campo en una validación).
 */
public record ApiError(String errorCode, String message, String requestId, Object details) {
}
