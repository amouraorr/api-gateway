package com.fiap.apigateway.infrastructure.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Security Config do API Gateway para profile 'docker' (reativo).
 *
 * Adiciona logs com fingerprint para facilitar debug de tokens entre serviços.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Profile("docker")
public class SecurityConfigDocker {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfigDocker.class);

    @Bean
    @Order(0)
    public SecurityWebFilterChain apiGatewaySecurityWebFilterChain(ServerHttpSecurity http) {
        logger.info("Ativando SecurityConfigDocker (API Gateway) - permissões definidas para profile 'docker'.");

        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Gateway: criação de encomendas somente para ROLE_PORTEIRO
                        .pathMatchers(HttpMethod.POST, "/api/parcels").hasRole("PORTEIRO")
                        .pathMatchers(HttpMethod.POST, "/api/parcels/*/pickup").hasRole("PORTEIRO")
                        .pathMatchers(HttpMethod.POST, "/api/parcels/*/confirm").hasRole("MORADOR")
                        // rotas internas de criação de usuário/login devem ser acessíveis
                        .pathMatchers(HttpMethod.POST, "/api/internal/**").permitAll()
                        .anyExchange().authenticated()
                )
                // Habilita validação JWT no gateway com conversor de roles
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwtSpec -> jwtSpec.jwtAuthenticationConverter(jwtAuthenticationConverterAdapter()))
                );

        return http.build();
    }

    /**
     * JwtDecoder HS256 (Nimbus) usando segredo Base64 ou texto simples como fallback.
     * Aceita tanto 'security.jwt.secret' quanto 'jwt.secret'.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${security.jwt.secret:${jwt.secret:}}") String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            logger.warn("security.jwt.secret / jwt.secret não definidos; JwtDecoder retornará erro ao decodificar tokens.");
            return token -> {
                throw new JwtException("JwtDecoder não configurado para o profile 'docker'. Defina 'security.jwt.secret' ou 'jwt.secret' para habilitar validação de tokens.");
            };
        }

        SecretKey secretKey = buildSecretKey(jwtSecret);
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Reactive JwtDecoder HS256 para WebFlux/Security reativo.
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(@Value("${security.jwt.secret:${jwt.secret:}}") String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            logger.warn("security.jwt.secret / jwt.secret não definidos; ReactiveJwtDecoder retornará erro ao decodificar tokens.");
            return token -> Mono.error(new JwtException("ReactiveJwtDecoder não configurado para o profile 'docker'. Defina 'security.jwt.secret' ou 'jwt.secret' para habilitar validação de tokens."));
        }

        SecretKey secretKey = buildSecretKey(jwtSecret);
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Converte claim 'roles' em GrantedAuthority com prefixo ROLE_ e adapta para uso reativo.
     */
    private ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverterAdapter() {
        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter((org.springframework.security.oauth2.jwt.Jwt jwt) -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) {
                return List.of();
            }
            return roles.stream()
                    .map(r -> "ROLE_" + r)
                    .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });
        return new ReactiveJwtAuthenticationConverterAdapter(conv);
    }

    /**
     * Resolve bytes para a chave HMAC (mesma lógica do UserService) e adiciona logs/fingerprint.
     */
    private SecretKey buildSecretKey(String secret) {
        byte[] keyBytes = resolveKeyBytes(secret);

        // fingerprint para comparação (SHA-256 sobre os bytes)
        String fp = computeFingerprintBase64(keyBytes);
        logger.info("ApiGateway: building SecretKey from provided secret. keyBytes.length={} fingerprint(SHA256-base64)={}", keyBytes.length, fp);

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    private byte[] resolveKeyBytes(String secret) {
        // tenta decodificar Base64
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            if (decoded.length >= 32) {
                logger.info("ApiGateway: secret provided as Base64 -> using decoded bytes ({} bytes).", decoded.length);
                return decoded;
            } else {
                logger.warn("ApiGateway: provided Base64 decoded to {} bytes (<32). Will fallback/derive.", decoded.length);
            }
        } catch (IllegalArgumentException ignored) {
            // não era Base64 -> prosseguir
        }

        // usa bytes UTF-8
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= 32) {
            logger.info("ApiGateway: using UTF-8 bytes of secret ({} bytes).", raw.length);
            return raw;
        }

        // derivar 32 bytes via SHA-256
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] derived = md.digest(raw); // 32 bytes
            logger.warn("ApiGateway: secret curto; deriving 32 bytes via SHA-256 for compatibility (NOT for production).");
            return derived;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available to derive JWT key bytes", e);
        }
    }

    private String computeFingerprintBase64(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(Arrays.hashCode(bytes));
        }
    }
}