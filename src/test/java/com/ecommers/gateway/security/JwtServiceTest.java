package com.ecommers.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias del servicio JWT del gateway.
 * No requiere mocks: se valida la generación y verificación de tokens de forma aislada.
 */
class JwtServiceTest {

    private static final String SECRET =
            "clave-secreta-de-al-menos-256-bits-para-pruebas-unitarias-0123456789";
    private static final String OTRO_SECRET =
            "otra-clave-distinta-de-al-menos-256-bits-para-pruebas-9876543210abcd";

    private final JwtService jwtService = new JwtService(SECRET, 3600);

    @Test
    @DisplayName("generateToken + parse: el token contiene subject, email y rol")
    void generateToken_yParse_devuelveLosClaims() {
        // When: se genera un token para el usuario 42 (ADMIN)
        String token = jwtService.generateToken("42", "ana@mail.com", "ADMIN");

        // Then: al parsearlo se recuperan los mismos claims
        Claims claims = jwtService.parse(token);
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("ana@mail.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("parse: un token con formato inválido lanza JwtException")
    void parse_tokenInvalido_lanzaExcepcion() {
        assertThatThrownBy(() -> jwtService.parse("esto-no-es-un-token"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("parse: un token firmado con otra clave es rechazado (firma inválida)")
    void parse_tokenConOtraFirma_lanzaExcepcion() {
        // Given: un token firmado con un secreto distinto
        JwtService otro = new JwtService(OTRO_SECRET, 3600);
        String tokenAjeno = otro.generateToken("1", "x@mail.com", "USER");

        // When / Then: este servicio no lo acepta
        assertThatThrownBy(() -> jwtService.parse(tokenAjeno))
                .isInstanceOf(JwtException.class);
    }
}
