package com.fiap.apigateway.infrastructure.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    private SecretKey keyFromSecret(String secret) {
        String effective = secret;
        if (effective == null || effective.trim().isEmpty()) {
            effective = "test-secret-fallback";
        }

        byte[] keyBytes = effective.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                keyBytes = digest.digest(keyBytes);
            } catch (Exception e) {
                keyBytes = Base64.getEncoder().encode(effective.getBytes(StandardCharsets.UTF_8));
            }
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    private void initWithSecret(String secret) throws Exception {
        Field f = JwtUtil.class.getDeclaredField("jwtSecret");
        f.setAccessible(true);
        f.set(jwtUtil, secret);
        jwtUtil.init();
    }

    @Test
    @DisplayName("Inicialização com segredo vazio deve usar fallback e validar token assinado com fallback")
    void testInitWithEmptySecret_usesFallbackSecret() throws Exception {
        // Arrange
        String fallback = "test-secret-fallback";
        initWithSecret("");

        SecretKey key = keyFromSecret(fallback);
        String token = Jwts.builder()
                .setSubject("usuario-fallback")
                .signWith(key)
                .compact();

        // Act
        Claims claims = jwtUtil.getClaims(token);

        // Assert
        assertNotNull(claims);
        assertEquals("usuario-fallback", claims.getSubject());
    }

    @Test
    @DisplayName("Inicialização com segredo curto deve derivar via SHA-256 e validar token assinado com chave derivada")
    void testInitWithShortSecret_derivesSha256() throws Exception {
        // Arrange
        String shortSecret = "short-secret";
        initWithSecret(shortSecret);

        SecretKey derivedKey = keyFromSecret(shortSecret);
        String token = Jwts.builder()
                .setSubject("usuario-curto")
                .signWith(derivedKey)
                .compact();

        // Act
        Claims claims = jwtUtil.getClaims(token);

        // Assert
        assertNotNull(claims);
        assertEquals("usuario-curto", claims.getSubject());
    }

    @Test
    @DisplayName("validateToken deve lançar JwtException para token assinado com chave diferente")
    void testValidateToken_invalidToken_throwsJwtException() throws Exception {
        // Arrange
        String realSecret = "real-secret-value-that-is-long-enough-for-hmac";
        initWithSecret(realSecret);

        SecretKey otherKey = keyFromSecret("other-secret-value-that-is-long-enough");
        String invalidToken = Jwts.builder()
                .setSubject("usuario-invalido")
                .signWith(otherKey)
                .compact();

        // Act & Assert
        assertThrows(JwtException.class, () -> jwtUtil.validateToken(invalidToken));
    }

    @Test
    @DisplayName("getClaims deve retornar Claims corretas para token válido")
    void testGetClaims_returnsClaims() throws Exception {
        // Arrange
        String secret = "another-very-long-secret-for-hmac-which-is-secure";
        initWithSecret(secret);

        SecretKey key = keyFromSecret(secret);
        String token = Jwts.builder()
                .setSubject("meu-usuario")
                .claim("role", "ADMIN")
                .signWith(key)
                .compact();

        // Act
        Claims claims = jwtUtil.getClaims(token);

        // Assert
        assertNotNull(claims);
        assertEquals("meu-usuario", claims.getSubject());
        assertEquals("ADMIN", claims.get("role"));
    }
}