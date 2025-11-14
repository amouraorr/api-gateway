package com.fiap.apigateway.infrastructure.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiGatewayLoggingFilterTest {

    @Test
    @DisplayName("Deve preservar X-Request-ID quando já presente na requisição")
    void preserveRequestIdWhenPresent() {
        // Arrange
        String existingId = "test-id-123";
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("X-Request-ID", existingId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        ApiGatewayLoggingFilter filter = new ApiGatewayLoggingFilter();

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();

        // Verify
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain, times(1)).filter(captor.capture());
        ServerWebExchange capturedExchange = captor.getValue();
        assertNotNull(capturedExchange);
        String forwardedId = capturedExchange.getRequest().getHeaders().getFirst("X-Request-ID");
        assertEquals(existingId, forwardedId, "O X-Request-ID existente deve ser preservado e repassado");
    }

    @Test
    @DisplayName("Deve adicionar X-Request-ID quando ausente e propagar erro do chain")
    void addRequestIdWhenAbsent() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.error(new RuntimeException("boom")));

        ApiGatewayLoggingFilter filter = new ApiGatewayLoggingFilter();

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .expectErrorMessage("boom")
                .verify();

        // Verify
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain, times(1)).filter(captor.capture());
        ServerWebExchange capturedExchange = captor.getValue();
        assertNotNull(capturedExchange);
        String addedId = capturedExchange.getRequest().getHeaders().getFirst("X-Request-ID");
        assertNotNull(addedId, "O X-Request-ID deve ser adicionado quando ausente");
        assertFalse(addedId.isBlank(), "O X-Request-ID adicionado não deve ser vazio");
    }
}