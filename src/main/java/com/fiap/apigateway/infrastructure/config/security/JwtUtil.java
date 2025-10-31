package com.fiap.apigateway.infrastructure.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Objects;

/**
 * Utilitário para validação de JWTs.
 *  - aceitar fallback via variável de ambiente SECURITY_JWT_SECRET,
 *  - suportar secret em base64 (prefixo "base64:"),
 *  - validar comprimento mínimo do secret e lançar erro informativo caso não configurado.
 */
@Component
public class JwtUtil {

    /**
     * Valor definido nos application-*.yml/properties ou via variável de ambiente SECURITY_JWT_SECRET.
     * Usar fallback vazio para evitar PlaceholderResolutionException.
     */
    @Value("${security.jwt.secret:}")
    private String jwtSecret;

    private Key key;

    @PostConstruct
    public void init() {
        // Se não foi definido no application-*, tentar ler da variável de ambiente
        if (jwtSecret == null || jwtSecret.isBlank()) {
            String env = System.getenv("SECURITY_JWT_SECRET");
            if (env != null && !env.isBlank()) {
                jwtSecret = env;
            }
        }

        // Se continuar vazio, falhar com mensagem clara (evita PlaceholderResolutionException obscuro)
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("Segredo JWT não configurado. Defina 'security.jwt.secret' em application.yml (ou application-docker.yml) "
                    + "ou defina a variável de ambiente 'SECURITY_JWT_SECRET'.");
        }

        byte[] secretBytes;

        // Suportar formato base64: se o valor começar com "base64:" interpretamos o restante como base64
        final String base64Prefix = "base64:";
        if (jwtSecret.startsWith(base64Prefix)) {
            String base64 = jwtSecret.substring(base64Prefix.length());
            try {
                secretBytes = Base64.getDecoder().decode(base64);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("O segredo JWT começa com 'base64:' mas não é um Base64 válido.", e);
            }
        } else {
            // Usar os bytes UTF-8 do secret (útil em ambiente local).
            secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }

        // Validar tamanho mínimo: jjwt para HS256/HS512 requer chave suficientemente longa para a HMAC.
        if (Objects.isNull(secretBytes) || secretBytes.length < 32) {
            throw new IllegalStateException("O segredo JWT configurado é muito curto. Forneça pelo menos 32 bytes (por exemplo, 32+ bytes codificados em base64). "
                    + "Use a variável de ambiente SECURITY_JWT_SECRET com valor no formato 'base64:<SEGREDO_BASE64>' ou defina uma string longa e aleatória na configuração.");
        }

        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    /**
     * Valida o token e retorna as Claims (ou lança exceção caso inválido).
     */
    public Jws<Claims> validateToken(String token) {
        // Pode lançar io.jsonwebtoken.JwtException se inválido
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }
}