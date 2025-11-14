package com.fiap.apigateway;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class StartupLoggerTest {

    @Test
    @DisplayName("onReady deve registrar 'default' quando não há perfis ativos")
    public void onReady_shouldLogDefaultWhenNoActiveProfiles() {
        // Arrange
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[0]);
        StartupLogger startupLogger = new StartupLogger(env);

        Logger logger = (Logger) LoggerFactory.getLogger(StartupLogger.class);
        @SuppressWarnings("unchecked")
        Appender<ILoggingEvent> appender = mock(Appender.class);
        when(appender.getName()).thenReturn("MOCK");
        logger.addAppender(appender);

        ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);

        // Act
        startupLogger.onReady();

        // Verify
        verify(env, times(1)).getActiveProfiles();
        verify(appender, times(1)).doAppend(captor.capture());

        // Assert
        ILoggingEvent event = captor.getValue();
        assertEquals(ch.qos.logback.classic.Level.INFO, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("default"));

        logger.detachAppender(appender);
    }

    @Test
    @DisplayName("onReady deve registrar 'ok' quando há perfis ativos")
    public void onReady_shouldLogOkWhenThereAreActiveProfiles() {
        // Arrange
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"dev", "auth"});
        StartupLogger startupLogger = new StartupLogger(env);

        Logger logger = (Logger) LoggerFactory.getLogger(StartupLogger.class);
        @SuppressWarnings("unchecked")
        Appender<ILoggingEvent> appender = mock(Appender.class);
        when(appender.getName()).thenReturn("MOCK");
        logger.addAppender(appender);

        ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);

        // Act
        startupLogger.onReady();

        // Verify
        verify(env, times(1)).getActiveProfiles();
        verify(appender, times(1)).doAppend(captor.capture());

        // Assert
        ILoggingEvent event = captor.getValue();
        assertEquals(ch.qos.logback.classic.Level.INFO, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("ok"));
        assertTrue(event.getFormattedMessage().contains("[dev, auth]"));

        logger.detachAppender(appender);
    }
}