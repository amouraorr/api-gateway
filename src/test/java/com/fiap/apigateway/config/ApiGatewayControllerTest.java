package com.fiap.apigateway.config;

import com.fiap.apigateway.adapter.web.ApiGatewayController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiGatewayControllerTest {

    @Mock
    private ServerHttpRequest request;

    @Mock
    private HttpHeaders headers;

    private final ApiGatewayController controller = new ApiGatewayController();

    @Test
    @DisplayName("Retorna OK quando existe X-Request-ID e remoteAddress presente")
    void health_withRequestIdAndRemoteAddress_returnsOk() {
        // Arrange
        InetSocketAddress remote = new InetSocketAddress("192.168.0.1", 12345);
        when(request.getRemoteAddress()).thenReturn(remote);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst("X-Request-ID")).thenReturn("req-123");

        // Act
        String result = controller.health(request);

        // Assert
        assertEquals("apigateway-ok", result);

        // Verify
        verify(request, times(1)).getRemoteAddress();
        verify(request, times(1)).getHeaders();
        verify(headers, times(1)).getFirst("X-Request-ID");
        verifyNoMoreInteractions(request, headers);
    }

    @Test
    @DisplayName("Retorna OK quando X-Request-ID ausente e remoteAddress nulo")
    void health_withoutRequestIdAndWithoutRemoteAddress_returnsOk() {
        // Arrange
        when(request.getRemoteAddress()).thenReturn(null);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst("X-Request-ID")).thenReturn(null);

        // Act
        String result = controller.health(request);

        // Assert
        assertEquals("apigateway-ok", result);

        // Verify
        verify(request, times(1)).getRemoteAddress();
        verify(request, times(1)).getHeaders();
        verify(headers, times(1)).getFirst("X-Request-ID");
        verifyNoMoreInteractions(request, headers);
    }
}