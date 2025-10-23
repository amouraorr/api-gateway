package com.fiap.apigateway.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PÓS GRADUAÇÃO - FIAP 2025 - SERVIÇO API Gateway")
                        .version("1.0.0")
                        .description("API Gateway responsável pelo roteamento, autenticação (JWT/OAuth2), políticas de segurança e documentação (OpenAPI) do sistema de gerenciamento de encomendas."));
    }
}