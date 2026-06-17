package com.ecommers.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Filtro global que valida el JWT en todas las rutas excepto las publicas.
 * Si el token es valido, propaga el id y el rol del usuario al microservicio
 * destino (cabeceras X-User-Id y X-User-Role) y aplica autorizacion por rol.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/auth/",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars",
            "/actuator/health"
    );

    private static final String ROLE_ADMIN = "ADMIN";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "Falta el token de autenticacion");
        }

        Claims claims;
        try {
            claims = jwtService.parse(authHeader.substring(7));
        } catch (JwtException ex) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "Token invalido o expirado");
        }

        String role = claims.get("role", String.class);
        if (role == null) {
            role = "USER";
        }

        String method = exchange.getRequest().getMethod().name();
        if (requiresAdmin(method, path) && !ROLE_ADMIN.equals(role)) {
            return writeError(exchange, HttpStatus.FORBIDDEN, "Se requiere rol ADMIN para esta operacion");
        }

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Role", role)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isPublic(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith)
                || path.contains("/v3/api-docs");
    }

    /**
     * Reglas de autorizacion: las escrituras sobre el catalogo y sobre los usuarios
     * son solo de ADMIN. El registro de usuarios va por /auth/register (publico),
     * que llama al servicio internamente sin pasar por este filtro.
     */
    private boolean requiresAdmin(String method, String path) {
        boolean isWrite = method.equals("POST") || method.equals("PUT")
                || method.equals("DELETE") || method.equals("PATCH");
        if (path.startsWith("/api/productos") && isWrite) {
            return true;
        }
        return path.startsWith("/api/v1/users") && isWrite;
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        byte[] body = ("{\"status\":" + status.value() + ",\"message\":\"" + message + "\"}").getBytes();
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
