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
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Configuração específica para o profile 'docker' do API Gateway (reativa).
 *
 * Observações:
 * - Liberamos explicitamente o POST /api/parcels para permitir testes via Gateway sem JWT.
 * - Esta configuração deve existir apenas para DEV/DOCKER. Em produção, remova esse profile.
 *
 * Alterações importantes:
 * - Adicionado @Order(0) para garantir que esta SecurityWebFilterChain tenha precedência
 *   sobre eventuais outras configurações de segurança e evitar 403 não esperado.
 * - Desabilitado explicitamente httpBasic e formLogin para evitar que filtros DEFAULT
 *   de autenticação recusem a requisição.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Profile("docker")
public class SecurityConfigDocker {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfigDocker.class);

    /**
     * Substitui a configuração servlet-based por uma configuração reativa compatível com WebFlux/Gateway.
     * usar ServerHttpSecurity e retornar SecurityWebFilterChain)
     */
    @Bean
    @Order(0)
    public SecurityWebFilterChain apiGatewaySecurityWebFilterChain(ServerHttpSecurity http) {
        logger.info("Ativando SecurityConfigDocker (API Gateway) - permissões relaxadas para profile 'docker'.");

        // Desabilita CSRF e autenticações padrão para ambiente de teste docker (ajuste conforme necessário)
        http
                .csrf(csrf -> csrf.disable())
                // desabilitar autenticação HTTP Basic e form login que podem ser habilitadas por default
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // endpoints públicos úteis em ambiente docker
                        .pathMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // permitir POSTS para criação de encomendas via API Gateway durante testes
                        .pathMatchers(HttpMethod.POST, "/api/parcels").permitAll()
                        // permitir rotas internas e criação de usuários se necessário:
                        .pathMatchers(HttpMethod.POST, "/api/internal/**").permitAll()
                        // Para TESTES LOCAIS: permitir todas as requisições
                        .anyExchange().permitAll()
                );

        // Observação: reative oauth2ResourceServer + jwtAuthenticationConverter em produção
        return http.build();
    }

    /**
     * JwtDecoder HS256 (Nimbus) usando segredo Base64 ou texto simples como fallback.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${security.jwt.secret:}") String jwtSecretBase64) {
        if (jwtSecretBase64 == null || jwtSecretBase64.isBlank()) {
            logger.warn("security.jwt.secret não definido; JwtDecoder retornará erro ao decodificar tokens.");
            return token -> {
                throw new JwtException("JwtDecoder não configurado para o profile 'docker'. Defina 'security.jwt.secret' para habilitar validação de tokens.");
            };
        }

        byte[] keyBytes;
        try {
            // tentar Base64
            keyBytes = Base64.getDecoder().decode(jwtSecretBase64);
            logger.info("JwtDecoder configurado com segredo fornecido como Base64 ({} bytes).", keyBytes.length);
        } catch (IllegalArgumentException ex) {
            // fallback para bytes UTF-8 do valor fornecido
            keyBytes = jwtSecretBase64.getBytes(StandardCharsets.UTF_8);
            logger.warn("Propriedade 'security.jwt.secret' não é um Base64 válido; usando bytes UTF-8 do valor fornecido ({} bytes).", keyBytes.length);
        }

        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}