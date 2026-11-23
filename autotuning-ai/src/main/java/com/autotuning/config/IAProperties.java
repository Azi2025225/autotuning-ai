package com.autotuning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Les clés API ne sont lues que depuis l'environnement (jamais dans le code ni dans les vues). */
@ConfigurationProperties(prefix = "app.ia")
public record IAProperties(String provider, String apiKey, String model, int timeoutSeconds) {
    public IAProperties {
        if (provider == null || provider.isBlank()) provider = "mock";
        if (timeoutSeconds <= 0) timeoutSeconds = 30;
    }
}
