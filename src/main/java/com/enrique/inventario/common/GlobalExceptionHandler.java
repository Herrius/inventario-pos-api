package com.enrique.inventario.common;

import com.enrique.inventario.user.EmailAlreadyExistsException;
import com.enrique.inventario.user.InvalidCredentialsException;
import com.enrique.inventario.user.UserNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Un único lugar que traduce excepciones a respuestas de error consistentes.
 * Evita try/catch repartidos por los controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleEmailExists(EmailAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage(), null);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.put(fe.getField(), fe.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Datos de entrada inválidos.", fields);
    }

    // NOTA: a propósito NO hay un @ExceptionHandler(Exception.class) catch-all.
    // Atrapaba excepciones del framework con status propio (p. ej.
    // NoResourceFoundException = 404) y las convertía en 500. Las excepciones no
    // manejadas las resuelve Spring con su código correcto. Unificar TODO al
    // formato ApiError (vía ResponseEntityExceptionHandler) es tarea de M6.

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message, Object details) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        return ResponseEntity.status(status).body(new ApiError(code, message, requestId, details));
    }
}
