package com.fiap.apigateway.config.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração programática das rotas. As URIs são lidas de properties (podem vir do application.yml).
 */
@Configuration
public class GatewayConfig {

    @Value("${services.user.uri:http://localhost:8081}")
    private String userServiceUri;

    @Value("${services.parcel.uri:http://localhost:8082}")
    private String parcelServiceUri;

    @Value("${services.auth.uri:http://localhost:8085}")
    private String authServiceUri;

    /**
     * Registra rotas programaticamente apenas quando a propriedade 'gateway.custom.routes.enabled' estiver ativada.
     */
    @Bean
    @ConditionalOnProperty(prefix = "gateway.custom.routes", name = "enabled", havingValue = "true", matchIfMissing = false)
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // ROTA PARA AUTH: encaminha /api/auth/** para auth-service, removendo o prefixo /api
                .route("auth-service", r -> r.path("/api/auth/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(authServiceUri))
                // ROTA INTERNA PARA USAR PELO GATEWAY: encaminha tudo que começa com /api/internal/** para o user-service
                // IMPORTANT: NÃO REMOVER O PREFIXO aqui, porque o user-service expõe endpoints como /api/internal/auth/login
                .route("user-service-internal", r -> r.path("/api/internal/**")
                        // sem stripPrefix: encaminha exatamente /api/internal/... -> backend espera mesmo isso
                        .uri(userServiceUri))
                // ROTA PÚBLICA DE USUÁRIOS: encaminha /api/users/** para user-service removendo o /api
                .route("user-service", r -> r.path("/api/users/**")
                        .filters(f -> f.stripPrefix(1)) // remove "api" -> destino recebe /users/...
                        .uri(userServiceUri))
                // ROTA DE ENCOMENDAS: encaminha /api/parcels/** para parcel-service removendo o /api
                .route("parcel-service", r -> r.path("/api/parcels/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(parcelServiceUri))
                .build();
    }
}