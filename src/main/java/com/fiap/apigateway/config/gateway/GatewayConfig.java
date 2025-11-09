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

    /**
     * Registra rotas programaticamente apenas quando a propriedade 'gateway.custom.routes.enabled' estiver ativada.
     */
    @Bean
    @ConditionalOnProperty(prefix = "gateway.custom.routes", name = "enabled", havingValue = "true", matchIfMissing = false)
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r.path("/api/users/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(userServiceUri))
                .route("parcel-service", r -> r.path("/api/parcels/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(parcelServiceUri))
                .build();
    }
}