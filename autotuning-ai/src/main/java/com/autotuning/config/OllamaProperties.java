package com.autotuning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ollama, en local (http://localhost:11434) ou en cloud (https://ollama.com + clé API).
 * La clé n'est lue que depuis l'environnement (OLLAMA_API_KEY).
 */
@ConfigurationProperties(prefix = "app.ollama")
public record OllamaProperties(String baseUrl, String apiKey, String textModel, String visionModel, int timeoutSeconds) {
    public OllamaProperties {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "http://localhost:11434";
        baseUrl = baseUrl.replaceAll("/+$", "");
        if (textModel == null || textModel.isBlank()) textModel = "llama3.1";
        if (timeoutSeconds <= 0) timeoutSeconds = 120;
    }

    /** La vérification des photos est désactivée tant qu'aucun modèle vision n'est configuré. */
    public boolean visionActive() { return visionModel != null && !visionModel.isBlank(); }
}
