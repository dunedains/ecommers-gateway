package com.ecommers.gateway.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del filtro global de autenticación JWT del gateway.
 * Se usa un exchange y un chain simulados (sin levantar el gateway).
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String SECRET =
            "clave-secreta-de-al-menos-256-bits-para-pruebas-unitarias-0123456789";

    private final JwtService jwtService = new JwtService(SECRET, 3600);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    @Mock
    private GatewayFilterChain chain;

    @Test
    @DisplayName("Ruta pública (/auth): pasa sin exigir token")
    void rutaPublica_pasa() {
        when(chain.filter(any())).thenReturn(Mono.empty());
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/auth/login").build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
    }

    @Test
    @DisplayName("Sin cabecera Authorization: responde 401 y no continúa")
    void sinToken_devuelve401() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders/1").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Token inválido: responde 401")
    void tokenInvalido_devuelve401() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-corrupto").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Token válido (USER): continúa y propaga X-User-Id / X-User-Role")
    void tokenValido_propagaCabeceras() {
        when(chain.filter(any())).thenReturn(Mono.empty());
        String token = jwtService.generateToken("42", "ana@mail.com", "USER");
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        var headers = captor.getValue().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("42");
        assertThat(headers.getFirst("X-User-Role")).isEqualTo("USER");
    }

    @Test
    @DisplayName("Escritura en /api/productos con rol USER: responde 403")
    void escrituraComoUser_devuelve403() {
        String token = jwtService.generateToken("42", "ana@mail.com", "USER");
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/productos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Escritura en /api/productos con rol ADMIN: continúa")
    void escrituraComoAdmin_pasa() {
        when(chain.filter(any())).thenReturn(Mono.empty());
        String token = jwtService.generateToken("1", "admin@mail.com", "ADMIN");
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/productos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
    }
}
