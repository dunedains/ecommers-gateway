package com.ecommers.gateway.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class AuthExceptionHandler {

    /** Propaga el codigo de estado devuelto por el microservicio de usuarios (401, 409, etc.). */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleDownstream(WebClientResponseException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        String message = switch (status) {
            case UNAUTHORIZED -> "Credenciales invalidas";
            case CONFLICT -> "El email ya esta registrado";
            default -> "Error en el servicio de usuarios";
        };
        return ResponseEntity.status(status).body(Map.of("status", status.value(), "message", message));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(WebExchangeBindException ex) {
        String message = ex.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of("status", 400, "message", message));
    }
}
