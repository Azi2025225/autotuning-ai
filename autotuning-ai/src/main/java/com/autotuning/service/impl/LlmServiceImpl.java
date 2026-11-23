package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.config.IAProperties;
import com.autotuning.exception.IAIndisponibleException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Accès unique au modèle de langage (Gemini, OpenAI ou Ollama) pour le conseiller moteur et le chatbot. */
@Service
public class LlmServiceImpl implements LlmService {
    private static final Logger log = LoggerFactory.getLogger(LlmServiceImpl.class);

    private final IAProperties props;
    private final OllamaClient ollama;
    private final RestClient http;

    public LlmServiceImpl(IAProperties props, OllamaClient ollama) {
        this.props = props;
        this.ollama = ollama;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(5));
        f.setReadTimeout(Duration.ofSeconds(props.timeoutSeconds()));
        this.http = RestClient.builder().requestFactory(f).build();
    }

    @Override
    public boolean modeDemo() { return "mock".equalsIgnoreCase(props.provider()); }

    @Override
    public String generer(String prompt, boolean json) {
        return switch (props.provider().toLowerCase()) {
            case "gemini" -> appelGemini(prompt, json);
            case "openai" -> appelOpenAi(prompt, json);
            case "ollama" -> ollama.chat(ollama.props().textModel(), prompt, List.of(), json);
            case "mock" -> throw new IAIndisponibleException("Mode démonstration : aucun modèle IA n'est configuré.");
            default -> throw new IAIndisponibleException("Fournisseur IA inconnu : " + props.provider());
        };
    }

    private void exigerCle() {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new IAIndisponibleException("L'IA n'est pas configurée (clé API manquante). Le reste du site reste disponible.");
        }
    }

    private String appelGemini(String prompt, boolean json) {
        exigerCle();
        String model = props.model() == null || props.model().isBlank() ? "gemini-2.0-flash" : props.model();
        Map<String, Object> config = new HashMap<>();
        config.put("temperature", 0.3);
        if (json) config.put("responseMimeType", "application/json");
        try {
            JsonNode r = http.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/{m}:generateContent", model)
                    .header("x-goog-api-key", props.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))), "generationConfig", config))
                    .retrieve().body(JsonNode.class);
            String t = r == null ? "" : r.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            if (t.isBlank()) throw new IAIndisponibleException("Réponse vide de l'API IA.");
            return t;
        } catch (RestClientException e) {
            log.warn("Échec appel Gemini : {}", e.getClass().getSimpleName());
            throw new IAIndisponibleException("Le service IA est momentanément indisponible ou trop lent. Réessayez dans quelques instants.", e);
        }
    }

    private String appelOpenAi(String prompt, boolean json) {
        exigerCle();
        String model = props.model() == null || props.model().isBlank() ? "gpt-4o-mini" : props.model();
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.3);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        if (json) body.put("response_format", Map.of("type", "json_object"));
        try {
            JsonNode r = http.post()
                    .uri("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + props.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve().body(JsonNode.class);
            String t = r == null ? "" : r.path("choices").path(0).path("message").path("content").asText("");
            if (t.isBlank()) throw new IAIndisponibleException("Réponse vide de l'API IA.");
            return t;
        } catch (RestClientException e) {
            log.warn("Échec appel OpenAI : {}", e.getClass().getSimpleName());
            throw new IAIndisponibleException("Le service IA est momentanément indisponible ou trop lent. Réessayez dans quelques instants.", e);
        }
    }
}
