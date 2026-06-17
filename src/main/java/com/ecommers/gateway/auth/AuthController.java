package com.ecommers.gateway.auth;

import com.ecommers.gateway.auth.AuthDtos.LoginRequest;
import com.ecommers.gateway.auth.AuthDtos.RegisterRequest;
import com.ecommers.gateway.auth.AuthDtos.TokenResponse;
import com.ecommers.gateway.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient userClient;
    private final JwtService jwtService;

    public AuthController(WebClient userServiceWebClient, JwtService jwtService) {
        this.userClient = userServiceWebClient;
        this.jwtService = jwtService;
    }

    /** Registra un usuario en el microservicio de usuarios y devuelve un JWT. */
    @PostMapping("/register")
    public Mono<ResponseEntity<TokenResponse>> register(@Valid @RequestBody RegisterRequest req) {
        Map<String, Object> payload = Map.of(
                "name", req.name(),
                "email", req.email(),
                "address", req.address(),
                "password", req.password());
        return userClient.post()
                .uri("/api/v1/users")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(MAP_TYPE)
                .map(this::toToken);
    }

    /** Valida credenciales contra el microservicio de usuarios y devuelve un JWT. */
    @PostMapping("/login")
    public Mono<ResponseEntity<TokenResponse>> login(@Valid @RequestBody LoginRequest req) {
        Map<String, Object> payload = Map.of(
                "email", req.email(),
                "password", req.password());
        return userClient.post()
                .uri("/api/v1/users/validate")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(MAP_TYPE)
                .map(this::toToken);
    }

    private ResponseEntity<TokenResponse> toToken(Map<String, Object> user) {
        Long id = ((Number) user.get("id")).longValue();
        String email = String.valueOf(user.get("email"));
        String token = jwtService.generateToken(String.valueOf(id), email);
        return ResponseEntity.ok(new TokenResponse(token, "Bearer", id, email));
    }
}
