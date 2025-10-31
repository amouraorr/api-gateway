package com.fiap.apigateway.infrastructure.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final JwtUtil jwtUtil;

    // Rotas que não precisam de autenticação (por exemplo, cadastro)
    private static final String[] PUBLIC_PATHS = {
            "/api/users/register",
            "/api/users/login",
            "/actuator"
    };

    public AuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // permitir rotas públicas sem token (passo do requisito: cadastro sem login)
        for (String pub : PUBLIC_PATHS) {
            if (path.startsWith(pub)) {
                return chain.filter(exchange);
            }
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            Jws<Claims> claimsJws = jwtUtil.validateToken(token);
            Claims claims = claimsJws.getBody();

            String userId = claims.getSubject();
            Object rolesObj = claims.get("roles");

            // Adiciona cabeçalhos para os serviços downstream
            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r.headers(h -> {
                        h.add("X-User-Id", userId != null ? userId : "");
                        h.add("X-Roles", rolesObj != null ? rolesObj.toString() : "");
                    }))
                    .build();

            return chain.filter(mutated);
        } catch (Exception ex) {
            logger.warn("JWT validation failed: {}", ex.getMessage());
            return unauthorized(exchange, "Invalid token: " + ex.getMessage());
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        byte[] bytes = message.getBytes();
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {

        return -100;
    }
}
