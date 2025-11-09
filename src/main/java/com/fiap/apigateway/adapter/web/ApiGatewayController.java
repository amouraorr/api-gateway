package com.fiap.apigateway.adapter.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Endpoint de health do API Gateway. Logs simples para confirmar acesso local.
 */
@RestController
@RequestMapping("/api/health")
public class ApiGatewayController {

    private static final Logger logger = LoggerFactory.getLogger(ApiGatewayController.class);

    @GetMapping
    public String health(ServerHttpRequest request) {
        String remote = Optional.ofNullable(request.getRemoteAddress()).map(Object::toString).orElse("unknown");
        String requestId = Optional.ofNullable(request.getHeaders().getFirst("X-Request-ID")).orElse("none");
        logger.info("[Health] id={} accessed from={}", requestId, remote);
        return "apigateway-ok";
    }
}