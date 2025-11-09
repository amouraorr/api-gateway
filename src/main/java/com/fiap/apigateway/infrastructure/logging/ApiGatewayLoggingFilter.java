package com.fiap.apigateway.infrastructure.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

/**
 * Filtro global do API Gateway que registra entrada e saída de requisições.
 */
@Component
public class ApiGatewayLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ApiGatewayLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String requestId = Optional.ofNullable(request.getHeaders().getFirst("X-Request-ID"))
                .orElse(UUID.randomUUID().toString());

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Request-ID", requestId)
                .build();
        exchange = exchange.mutate().request(mutatedRequest).build();

        String method = Optional.ofNullable(mutatedRequest.getMethod()).map(Object::toString).orElse("UNKNOWN");
        String uri = mutatedRequest.getURI().toString();
        String remote = Optional.ofNullable(mutatedRequest.getRemoteAddress()).map(Object::toString).orElse("unknown");
        logger.info("[Gateway-Request] id={} {} {} from={} headers={}",
                requestId, method, uri, remote, maskSensitiveHeaders(mutatedRequest.getHeaders().toString()));

        ServerHttpResponse response = exchange.getResponse();

        return chain.filter(exchange)
                .doOnSuccess(aVoid -> {
                    String status = "UNKNOWN_STATUS";
                    if (response.getStatusCode() != null) {
                        status = String.valueOf(response.getStatusCode().value());
                    }
                    logger.info("[Gateway-Response] id={} {} {} -> status={} forwardedFor={}",
                            requestId, method, uri, status, response.getHeaders().getFirst("X-Forwarded-For"));
                })
                .doOnError(throwable -> {
                    logger.error("[Gateway-Response] id={} {} {} -> ERROR: {}", requestId, method, uri, throwable.toString());
                });
    }

    @Override
    public int getOrder() {

        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String maskSensitiveHeaders(String headers) {
       
        return headers.replaceAll("(?i)authorization=[^,}]+", "authorization=***")
                      .replaceAll("(?i)cookie=[^,}]+", "cookie=***");
    }
}