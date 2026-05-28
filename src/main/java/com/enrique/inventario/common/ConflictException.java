package com.enrique.inventario.common;

/** Conflicto de estado (p. ej. nombre duplicado). El handler global la mapea a 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
