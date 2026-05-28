package com.enrique.inventario.common;

/** Recurso inexistente. El handler global la mapea a 404. Genérica para todo el catálogo. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
