package com.enrique.inventario.user;

/** Usuario inexistente (p. ej. token válido de un usuario ya borrado). Mapea a 404. */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String email) {
        super("Usuario no encontrado: " + email);
    }
}
