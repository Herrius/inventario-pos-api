package com.enrique.inventario.common;

import com.enrique.inventario.user.EmailAlreadyExistsException;
import com.enrique.inventario.user.InvalidCredentialsException;
import com.enrique.inventario.user.UserNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Único punto que traduce excepciones a respuestas {@link ApiError} consistentes.
 *
 * Extiende {@link ResponseEntityExceptionHandler} para reescribir las respuestas que
 * Spring genera por defecto (404, 405, 415, 400 de body malformado, etc.). Sin esto,
 * los errores del framework salen como {@code ProblemDetail} mientras los nuestros
 * salen como {@code ApiError}; el cliente recibe dos formatos distintos.
 *
 * Política:
 *  - Excepciones del DOMINIO → un {@code @ExceptionHandler} explícito con un
 *    {@code errorCode} estable y máquina-legible.
 *  - Excepciones del FRAMEWORK → se canalizan vía {@code handleExceptionInternal}
 *    y un mapeo a {@code errorCode}.
 *  - SIN catch-all genérico de {@code Exception.class}: enmascara bugs reales.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // --------- Excepciones del dominio ---------

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleEmailExists(EmailAlreadyExistsException ex) {
        return apiError(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex) {
        return apiError(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage(), null);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex) {
        return apiError(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        return apiError(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return apiError(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), null);
    }

    // --------- Overrides de ResponseEntityExceptionHandler ---------
    // Estos overrides garantizan que las respuestas del framework también salgan
    // como ApiError (mismo shape que las del dominio).

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.put(fe.getField(), fe.getDefaultMessage());
        }
        return wrap(status, "VALIDATION_ERROR", "Datos de entrada inválidos.", fields, headers);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        // Si una sub-clase ya construyó un body custom, lo respetamos.
        if (body instanceof ApiError) {
            return new ResponseEntity<>(body, headers, statusCode);
        }
        return wrap(statusCode, deriveErrorCode(ex, statusCode), ex.getMessage(), null, headers);
    }

    /**
     * Mapea excepciones conocidas del framework a códigos máquina-legibles estables.
     * Para todo lo demás, usa {@code FRAMEWORK_ERROR_<status>} como fallback.
     */
    private String deriveErrorCode(Exception ex, HttpStatusCode statusCode) {
        if (ex instanceof NoResourceFoundException) return "RESOURCE_NOT_FOUND";
        if (ex instanceof HttpMessageNotReadableException) return "MALFORMED_REQUEST";
        if (ex instanceof HttpMediaTypeNotSupportedException) return "UNSUPPORTED_MEDIA_TYPE";
        if (ex instanceof HttpRequestMethodNotSupportedException) return "METHOD_NOT_ALLOWED";
        return "FRAMEWORK_ERROR_" + statusCode.value();
    }

    private ResponseEntity<Object> wrap(
            HttpStatusCode status, String code, String message,
            Object details, HttpHeaders headers) {
        ApiError body = new ApiError(code, message, newRequestId(), details);
        return new ResponseEntity<>(body, headers, status);
    }

    private ResponseEntity<ApiError> apiError(
            HttpStatus status, String code, String message, Object details) {
        return ResponseEntity.status(status).body(new ApiError(code, message, newRequestId(), details));
    }

    private String newRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
