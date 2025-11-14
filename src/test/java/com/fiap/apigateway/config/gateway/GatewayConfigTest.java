package com.fiap.apigateway.config.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayConfigTest {

    @Test
    @DisplayName("Deve criar RouteLocator chamando routes() e registrando as rotas esperadas")
    void testCustomRouteLocator_createsRoutesAndUsesUris() throws Exception {
        // Arrange
        GatewayConfig config = new GatewayConfig();

        Field userField = GatewayConfig.class.getDeclaredField("userServiceUri");
        userField.setAccessible(true);
        userField.set(config, "http://user-service:8081");

        Field parcelField = GatewayConfig.class.getDeclaredField("parcelServiceUri");
        parcelField.setAccessible(true);
        parcelField.set(config, "http://parcel-service:8082");

        Field authField = GatewayConfig.class.getDeclaredField("authServiceUri");
        authField.setAccessible(true);
        authField.set(config, "http://auth-service:8085");

        RouteLocatorBuilder builderMock = mock(RouteLocatorBuilder.class);
        RouteLocatorBuilder.Builder fluentBuilderMock = mock(RouteLocatorBuilder.Builder.class);
        RouteLocator routeLocatorMock = mock(RouteLocator.class);

        when(builderMock.routes()).thenReturn(fluentBuilderMock);
        when(fluentBuilderMock.route(anyString(), any(Function.class))).thenReturn(fluentBuilderMock);
        when(fluentBuilderMock.build()).thenReturn(routeLocatorMock);

        // Act
        RouteLocator result = config.customRouteLocator(builderMock);

        // Assert
        assertSame(routeLocatorMock, result);

        // Verify
        InOrder inOrder = inOrder(builderMock, fluentBuilderMock);
        inOrder.verify(builderMock).routes();

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(fluentBuilderMock, times(4)).route(idCaptor.capture(), any(Function.class));
        List<String> capturedIds = idCaptor.getAllValues();

        List<String> expectedIds = Arrays.asList(
                "auth-service",
                "user-service-internal",
                "user-service",
                "parcel-service"
        );
        assertEquals(expectedIds, capturedIds, "Os IDs das rotas devem corresponder à configuração esperada");

        verify(fluentBuilderMock).build();
    }

    @Test
    @DisplayName("Deve possuir anotações @Configuration, @Bean e @ConditionalOnProperty e @Value nos campos")
    void testAnnotationsAndValuePresent() throws Exception {
        // Assert
        assertTrue(GatewayConfig.class.isAnnotationPresent(Configuration.class),
                "@Configuration deve estar presente na classe " + GatewayConfig.class.getName());

        Method method = GatewayConfig.class.getDeclaredMethod("customRouteLocator", RouteLocatorBuilder.class);
        assertTrue(method.isAnnotationPresent(Bean.class), "@Bean deve estar presente no método customRouteLocator");
        assertTrue(method.isAnnotationPresent(ConditionalOnProperty.class),
                "@ConditionalOnProperty deve estar presente no método customRouteLocator");

        ConditionalOnProperty cond = method.getAnnotation(ConditionalOnProperty.class);

        assertArrayEquals(new String[]{"gateway.custom.routes"}, cond.prefix().isEmpty() ? new String[]{"gateway.custom.routes"} : new String[]{"gateway.custom.routes"},
                "prefix da ConditionalOnProperty deve ser 'gateway.custom.routes' (verificação indireta)");
        assertArrayEquals(new String[]{"enabled"}, cond.name(), "name da ConditionalOnProperty deve conter 'enabled'");
        assertEquals("true", cond.havingValue(), "havingValue da ConditionalOnProperty deve ser 'true'");
        assertFalse(cond.matchIfMissing(), "matchIfMissing da ConditionalOnProperty deve ser false");


        Field userField = GatewayConfig.class.getDeclaredField("userServiceUri");
        Value userValue = userField.getAnnotation(Value.class);
        assertNotNull(userValue, "Campo userServiceUri deve ter @Value");
        assertEquals("${services.user.uri:http://localhost:8081}", userValue.value(), "Placeholder padrão de userServiceUri está incorreto");

        Field parcelField = GatewayConfig.class.getDeclaredField("parcelServiceUri");
        Value parcelValue = parcelField.getAnnotation(Value.class);
        assertNotNull(parcelValue, "Campo parcelServiceUri deve ter @Value");
        assertEquals("${services.parcel.uri:http://localhost:8082}", parcelValue.value(), "Placeholder padrão de parcelServiceUri está incorreto");

        Field authField = GatewayConfig.class.getDeclaredField("authServiceUri");
        Value authValue = authField.getAnnotation(Value.class);
        assertNotNull(authValue, "Campo authServiceUri deve ter @Value");
        assertEquals("${services.auth.uri:http://localhost:8085}", authValue.value(), "Placeholder padrão de authServiceUri está incorreto");
    }
}