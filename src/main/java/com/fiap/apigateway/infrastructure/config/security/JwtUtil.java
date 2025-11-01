package com.fiap.apigateway.infrastructure.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // Tenta ler de application.yml: security.jwt.secret ou da variável de ambiente SECURITY_JWT_SECRET
    @Value("${security.jwt.secret:${SECURITY_JWT_SECRET:}}")
    private String jwtSecret;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            logger.warn("Segredo JWT não configurado. Usando fallback temporário (não usar em produção).");
            jwtSecret = "test-secret-fallback";
        }

        // Garante tamanho mínimo para HMAC-SHA-256 (32 bytes). Se muito curto, deriva com SHA-256.
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                keyBytes = digest.digest(keyBytes);
            } catch (Exception e) {
                // fallback: base64-encode a string para aumentar o tamanho
                logger.warn("Falha ao derivar chave com SHA-256, aplicando Base64 fallback: {}", e.getMessage());
                keyBytes = Base64.getEncoder().encode(jwtSecret.getBytes(StandardCharsets.UTF_8));
            }
        }

        secretKey = Keys.hmacShaKeyFor(keyBytes);
        logger.info("JwtUtil inicializado com chave HMAC de tamanho {} bytes.", keyBytes.length);
    }

    /**
     * Valida e faz o parse do token JWT.
     * Lança JwtException (ou subclasses) em tokens inválidos/expirados/assinatura inválida.
     */
    public Jws<Claims> validateToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
    }

    /**
     * Conveniência: retorna Claims extraídas do token (lança JwtException se inválido).
     */
    public Claims getClaims(String token) throws JwtException {
        return validateToken(token).getBody();
    }
}