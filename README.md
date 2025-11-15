Reconhecido — vou ajustar o README do módulo api-gateway com base no padrão fornecido e no código já implementado.

Plano de implementação
1. Estruturar o README seguindo o template do exemplo (cabeçalho, introdução, objetivo, requisitos, estrutura, segurança, arquitetura, princípios de design, interação, tecnologias, pré-requisitos e execução, endpoints, contribuição e referências).
2. Substituir referências ao serviço de produto por descrições e responsabilidades do API Gateway (roteamento, autenticação JWT/OAuth2, logging, OpenAPI).
3. Mapear a estrutura do projeto para os pacotes/arquivos reais e linkar as classes/métodos relevantes do código (usar os links psi_element:// exigidos para Java).
4. Incluir instruções básicas para executar com Docker Compose e destacar endpoints principais (ex.: health).
5. Validar que o README mencione as classes utilitárias de JWT, filtros e configuração de segurança já implementadas e forneça links para elas.

Explicação rápida sobre o que será feito no arquivo:
- Vou gerar um README.md alinhado ao padrão informado, adaptado ao serviço "api-gateway", e adicionar links para as classes e métodos concretos existentes no projeto para facilitar navegação no IDE (conforme as regras).

Abaixo está o README completo modificado para o módulo api-gateway.

Breve descrição: README atualizado para o serviço api-gateway; inclui visão geral, requisitos, estrutura do projeto e links diretos para as classes e métodos relevantes do código-fonte.

```md:E:/00000000000 - PÓS GRADUAÇÃO FIAP 2025/ProjetoFiapGerenciamentoEncomendas/api-gateway/README.md
# Serviço de Api-Gateway - Backend

## Introdução

Este microsserviço atua como o API Gateway do sistema de gerenciamento de encomendas. Ele é responsável pelo roteamento das requisições para os serviços internos, autenticação e validação de tokens JWT/OAuth2, aplicação de políticas transversais (como logging e propagação de request-id) e pela publicação da documentação OpenAPI.

## Objetivo do Projeto

O objetivo principal deste serviço é centralizar responsabilidades cross-cutting (roteamento, segurança, logging e documentação) para os microsserviços do ecossistema, facilitando monitoramento, controle de acesso e padronização das APIs.

## Requisitos do Sistema

Para executar este microsserviço, você precisará dos seguintes requisitos:

- Sistema Operacional: Windows, macOS ou Linux
- Memória RAM: Pelo menos 4 GB recomendados
- Espaço em Disco: Pelo menos 500 MB de espaço livre
- Software:
  - Docker e Docker Compose
  - Java JDK 11 ou superior
  - Maven 3.6 ou superior
  - Git

## Estrutura do Projeto

A estrutura está organizada para refletir responsabilidades de gateway e infraestrutura, com estas pastas principais:
```plaintext
api-gateway/
│
├── src/
│ └── main/
│   ├── java/
│   │ └── com.fiap.apigateway
│   │   ├── adapter/            : Controladores / endpoints do gateway.
│   │   ├── config/             : Configurações gerais e de gateway.
│   │   │   └── gateway/        : rotas programáticas.
│   │   ├── infrastructure/
│   │   │   └── config/security/ : Configurações de JWT/segurança.
│   │   │   └── logging/        : Filtros e interceptadores.
│   │   ├── config/swagger/     : OpenAPI config.
│   │   └── ApiGatewayApplication.java : Classe principal da aplicação.
│   └── resources/
│       └── application.yml/properties : Configurações da aplicação.
├── pom.xml
├── Dockerfile
├── docker-compose.yml
└── README.md

## Segurança

A segurança é baseada em Spring Security e Spring Security OAuth2 Resource Server. O gateway:
- Aceita tokens JWT assinados por HS256 (quando configurado um secret) ou valida via issuer/JWKS quando está definido.
- Converte claims "roles" / "role" em authorities usando [JwtRoleConverter].
- Há uma configuração específica para execução em ambiente `docker` via [SecurityConfigDocker] e outra para WebFlux via [SecurityConfig].
- A leitura/decodificação de chaves/secret é feita por [JwtConfig] e a derivação de chaves HMAC por [JwtKeyUtil].

## Visão Geral do Projeto

O API Gateway foi desenvolvido com Spring Boot (WebFlux + Spring Cloud Gateway) e provê:
- Roteamento de requisições para serviços internos (configuráveis via.
- Validação e conversão de tokens JWT (HS256 / RS256 via JWKS).
- Filtro global para logging e propagação de X-Request-ID.
- Endpoints públicos de health e debug .

## Arquitetura

Padrão arquitetural adotado:
- MVC para a camada de entrada (controllers) e configurações reativas do Gateway.
- Separation of concerns entre:
  - Roteamento ([GatewayConfig]
  - Segurança ([SecurityConfig]
  - Logging ([ApiGatewayLoggingFilter]
  - Documentação ([OpenApiSwaggerConfig#customOpenAPI]

## Princípios de Design e Padrões de Projeto

- Single Responsibility Principle (SRP): cada classe tem responsabilidade bem definida.
- Gateway Pattern: abstração das rotas e filtros no Gateway.
- Converter Pattern: transformação de claims para authorities via [JwtRoleConverter].

## Interação entre as Partes do Sistema

1. Cliente envia requisições ao Gateway.
2. Gateway valida autenticação/autorização.
3. Gateway aplica filtros transversais.
4. Gateway realiza roteamento para o serviço apropriado conforme regras.
5. Serviços internos respondem e o Gateway encaminha a resposta ao cliente.

## Tecnologias Utilizadas

- Spring Boot (WebFlux)
- Spring Cloud Gateway
- Spring Security / OAuth2 Resource Server
- Reactor (WebFlux)
- MapStruct, Lombok (quando aplicável em outros módulos)
- Swagger (Springdoc OpenAPI) — configurado em [OpenApiSwaggerConfig#customOpenAPI]
- Docker e Docker Compose

## Pré-requisitos

- Docker e Docker Compose
- Java JDK 11 ou superior
- Maven 3.6 ou superior

### Executando com Docker Compose (exemplo)

1. Garanta que Docker e Docker Compose estejam instalados.
2. No diretório do módulo `api-gateway`, execute:
   ```bash
   docker compose up
   ```
3. A aplicação estará disponível em Swagger em `http://localhost:8085/webjars/swagger-ui/index.html#/`.( obs: confirmação de está rodando no docker-compose.yml)

## Endpoints Principais

- GET /api/health — health do gateway (implementado em [ApiGatewayController#health]
- Rotas de proxy para serviços internos configuradas em [GatewayConfig#customRouteLocator]

## Contribuição

Contribuições são bem-vindas. Fluxo sugerido:
1. Faça um fork do repositório.
2. Crie uma branch para sua feature: `git checkout -b feature/nome-da-feature`.
3. Faça commit das alterações: `git commit -m "Descrição da feature"`.
4. Envie para o repositório remoto: `git push origin feature/nome-da-feature`.
5. Abra um Pull Request.

## Licença

Este projeto é privado ou não possui licença específica.

## Referências e Recursos

- Spring Boot: https://spring.io/projects/spring-boot
- Spring Security: https://spring.io/projects/spring-security
- Spring Cloud Gateway: https://spring.io/projects/spring-cloud-gateway
- Spring Data / OAuth2 / JWT: https://spring.io
- Springdoc OpenAPI (Swagger): https://springdoc.org/

## Conclusão

Este API Gateway centraliza responsabilidades transversais do sistema de gerenciamento de encomendas, implementando boas práticas de segurança, roteamento e observabilidade para facilitar a manutenção e integração entre os microsserviços.
