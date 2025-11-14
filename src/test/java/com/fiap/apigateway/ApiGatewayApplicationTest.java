package com.fiap.apigateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ApiGatewayApplicationTest {

    @Test
    @DisplayName("main chama SpringApplication.run com os argumentos e classe corretos")
    void testMainShouldCallSpringApplicationRun() {
        // Arrange
        String[] args = {"arg1", "arg2"};
        ConfigurableApplicationContext mockContext = Mockito.mock(ConfigurableApplicationContext.class);

        try (MockedStatic<SpringApplication> mocked = Mockito.mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(ApiGatewayApplication.class, args)).thenReturn(mockContext);

            // Act & Assert
            assertDoesNotThrow(() -> ApiGatewayApplication.main(args));

            // Verify
            mocked.verify(() -> SpringApplication.run(ApiGatewayApplication.class, args), Mockito.times(1));
        }
    }
}