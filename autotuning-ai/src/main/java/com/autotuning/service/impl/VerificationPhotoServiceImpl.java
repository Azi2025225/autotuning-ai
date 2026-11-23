package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.config.OllamaProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

/**
 * Vérifie, avec un modèle vision Ollama, qu'une photo importée montre bien une voiture et la décrit.
 * Désactivé sans modèle vision configuré ; en cas d'échec technique la vérification est ignorée (le site reste utilisable).
 */
@Service
public class VerificationPhotoServiceImpl implements VerificationPhotoService {
    private static final Logger log = LoggerFactory.getLogger(VerificationPhotoServiceImpl.class);

    private final OllamaClient ollama;
    private final OllamaProperties props;
    private final ObjectMapper mapper;

    public VerificationPhotoServiceImpl(OllamaClient ollama, OllamaProperties props, ObjectMapper mapper) {
        this.ollama = ollama; this.props = props; this.mapper = mapper;
    }

    @Override
    public Optional<Resultat> analyser(BufferedImage image) {
        if (!props.visionActive()) return Optional.empty();
        try {
            byte[] jpg = StockageImageService.enJpeg(StockageImageService.normaliser(image, 768));
            String reponse = ollama.chat(props.visionModel(),
                    "Look at this image. Answer ONLY with JSON: {\"voiture\": true or false, \"description\": \"make, model, colour and view angle in at most 15 words, or empty\"}. "
                            + "\"voiture\" is true only if the main subject is a car.",
                    List.of(jpg), true);
            int a = reponse.indexOf('{'), b = reponse.lastIndexOf('}');
            if (a < 0 || b <= a) return Optional.empty();
            JsonNode n = mapper.readTree(reponse.substring(a, b + 1));
            if (!n.has("voiture")) return Optional.empty();
            String d = n.path("description").asText("");
            return Optional.of(new Resultat(n.path("voiture").asBoolean(true), d.length() > 120 ? d.substring(0, 120) : d));
        } catch (Exception e) {
            log.warn("Vérification de la photo ignorée : {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
