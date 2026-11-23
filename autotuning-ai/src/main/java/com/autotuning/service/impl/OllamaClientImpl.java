package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.config.OllamaProperties;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client minimal de l'API Ollama (/api/chat), texte et vision. */
@Service
public class OllamaClientImpl implements OllamaClient {
    private static final Logger log = LoggerFactory.getLogger(OllamaClientImpl.class);

    private final OllamaProperties props;
    private final RestClient http;

    public OllamaClientImpl(OllamaProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(5));
        f.setReadTimeout(Duration.ofSeconds(props.timeoutSeconds()));
        this.http = RestClient.builder().requestFactory(f).build();
    }

    @Override
    public OllamaProperties props() { return props; }

    @Override
    public String chat(String model, String prompt, List<byte[]> images, boolean json) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        if (images != null && !images.isEmpty()) {
            message.put("images", images.stream().map(b -> Base64.getEncoder().encodeToString(b)).toList());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("stream", false);
        body.put("messages", List.of(message));
        body.put("options", Map.of("temperature", 0.3));
        if (json) body.put("format", "json");

        try {
            RestClient.RequestBodySpec req = http.post().uri(props.baseUrl() + "/api/chat").contentType(MediaType.APPLICATION_JSON);
            if (props.apiKey() != null && !props.apiKey().isBlank()) req = req.header("Authorization", "Bearer " + props.apiKey());
            JsonNode r = req.body(body).retrieve().body(JsonNode.class);
            String t = r == null ? "" : r.path("message").path("content").asText("");
            if (t.isBlank()) throw new IAIndisponibleException("Réponse vide du modèle Ollama.");
            return t;
        } catch (RestClientException e) {
            log.warn("Échec appel Ollama ({}) : {}", model, e.getClass().getSimpleName());
            throw new IAIndisponibleException("Ollama est injoignable ou le modèle « " + model
                    + " » n'est pas disponible. Vérifiez qu'Ollama est lancé et que le modèle est installé.", e);
        }
    }
}
