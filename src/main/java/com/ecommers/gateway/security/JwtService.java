package com.ecommers.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey key;
    private final long ttlMillis;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.ttl-seconds:3600}") long ttlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = ttlSeconds * 1000;
    }

    public String generateToken(String userId, String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMillis))
                .signWith(key)
                .compact();
    }

    /** Devuelve los claims si el token es valido; lanza JwtException si no lo es. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
