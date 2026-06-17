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
 * Si el token es valido, propaga el id del usuario al microservicio destino
 * mediante la cabecera interna X-User-Id.
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
            return unauthorized(exchange, "Falta el token de autenticacion");
        }

        try {
            Claims claims = jwtService.parse(authHeader.substring(7));
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-User-Id", claims.getSubject())
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (JwtException ex) {
            return unauthorized(exchange, "Token invalido o expirado");
        }
    }

    private boolean isPublic(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith)
                || path.contains("/v3/api-docs");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        byte[] body = ("{\"status\":401,\"message\":\"" + message + "\"}").getBytes();
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
