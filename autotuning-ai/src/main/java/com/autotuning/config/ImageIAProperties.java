package com.autotuning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Édition d'image par IA (Gemini). La clé n'est lue que depuis l'environnement (IA_IMAGE_API_KEY). */
@ConfigurationProperties(prefix = "app.ia-image")
public record ImageIAProperties(String apiKey, String model, int timeoutSeconds) {
    public ImageIAProperties {
        if (model == null || model.isBlank()) model = "gemini-2.5-flash-image";
        if (timeoutSeconds <= 0) timeoutSeconds = 120;
    }

    public boolean actif() { return apiKey != null && !apiKey.isBlank(); }
}
