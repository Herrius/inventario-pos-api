package com.enrique.inventario.user;

/** Se lanza al registrar un email que ya existe. El handler global la mapea a 409. */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("El email ya está registrado: " + email);
    }
}
