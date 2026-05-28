package com.enrique.inventario.user;

/** Credenciales de login inválidas. El handler global la mapea a 401. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Credenciales inválidas.");
    }
}
