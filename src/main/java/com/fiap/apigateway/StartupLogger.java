package com.fiap.apigateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/*
 * StartupLogger registra explicitamente a aplicação subiu (ajuda na triagem) e registra também um warning com perfis ativos.
 */

@Component
public class StartupLogger {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);
    private final Environment env;

    public StartupLogger(Environment env) {
        this.env = env;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String[] profiles = env.getActiveProfiles();
        log.info("Api Gateway started. activeProfiles={} ({}).", Arrays.toString(profiles), profiles.length == 0 ? "default" : "ok");
    }
}