package com.ecommers.gateway.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank(message = "El nombre es obligatorio") String name,
            @NotBlank(message = "El email es obligatorio") @Email(message = "Email invalido") String email,
            @NotBlank(message = "La direccion es obligatoria") String address,
            @NotBlank(message = "La contrasena es obligatoria") String password
    ) {}

    public record LoginRequest(
            @NotBlank(message = "El email es obligatorio") @Email(message = "Email invalido") String email,
            @NotBlank(message = "La contrasena es obligatoria") String password
    ) {}

    public record TokenResponse(
            String token,
            String tokenType,
            Long userId,
            String email,
            String role
    ) {}
}
