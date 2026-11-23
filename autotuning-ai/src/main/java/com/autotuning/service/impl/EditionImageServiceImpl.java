package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.config.ImageIAProperties;
import com.autotuning.exception.IAIndisponibleException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Édition d'image par IA (Gemini) : applique un kit/une pièce du catalogue sur la photo d'une voiture. */
@Service
public class EditionImageServiceImpl implements EditionImageService {
    private static final Logger log = LoggerFactory.getLogger(EditionImageServiceImpl.class);

    private final ImageIAProperties props;
    private final RestClient http;

    public EditionImageServiceImpl(ImageIAProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(10));
        f.setReadTimeout(Duration.ofSeconds(props.timeoutSeconds()));
        this.http = RestClient.builder().requestFactory(f).build();
    }

    @Override
    public boolean disponible() { return props.actif(); }

    @Override
    public BufferedImage appliquerKit(BufferedImage voiture, Optional<byte[]> photoKit, String nomPiece, String categorie, String vehicule) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt(nomPiece, categorie, vehicule, photoKit.isPresent())));
        parts.add(image(StockageImageService.enJpeg(StockageImageService.normaliser(voiture, 1280)), "image/jpeg"));
        photoKit.ifPresent(b -> parts.add(image(b, b.length > 3 && (b[0] & 0xFF) == 0x89 ? "image/png" : "image/jpeg")));
        return editer(parts);
    }

    @Override
    public BufferedImage appliquerCouleur(BufferedImage voiture, String couleur, String vehicule) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", "You are a photorealistic automotive image editor. The image shows a car"
                + (vehicule == null || vehicule.isBlank() ? "" : " (" + vehicule + ")") + ". "
                + "Repaint ONLY the car body in a realistic glossy " + couleur + " paint, keeping the reflections, shadows and highlights natural. "
                + "Keep everything else identical: same car, angle, wheels, glass, lights, badges, background and framing. "
                + "Do not add text, logos or watermarks. Return only the edited image."));
        parts.add(image(StockageImageService.enJpeg(StockageImageService.normaliser(voiture, 1280)), "image/jpeg"));
        return editer(parts);
    }

    private BufferedImage editer(List<Map<String, Object>> parts) {
        if (!props.actif()) throw new IAIndisponibleException("L'édition d'image par IA n'est pas configurée (clé API manquante).");
        try {
            JsonNode r = http.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/{m}:generateContent", props.model())
                    .header("x-goog-api-key", props.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("contents", List.of(Map.of("parts", parts)),
                            "generationConfig", Map.of("responseModalities", List.of("TEXT", "IMAGE"))))
                    .retrieve().body(JsonNode.class);
            return extraireImage(r);
        } catch (RestClientResponseException e) {
            // Le corps de la réponse peut contenir des détails utiles ; il n'est journalisé qu'en partie et la clé n'y figure jamais.
            String corps = e.getResponseBodyAsString();
            log.warn("Échec édition d'image Gemini : HTTP {} {}", e.getStatusCode().value(), corps.length() > 300 ? corps.substring(0, 300) : corps);
            throw new IAIndisponibleException(messagePourStatut(e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            log.warn("Échec édition d'image Gemini : {}", e.getClass().getSimpleName());
            throw new IAIndisponibleException("Le service d'édition d'image est injoignable ou trop lent. Réessayez dans un instant.", e);
        }
    }

    /** Message précis selon le code HTTP, pour que l'administrateur sache quoi corriger (clé, quota, modèle). */
    static String messagePourStatut(int statut) {
        return switch (statut) {
            case 400 -> "Demande refusée par le service d'édition d'image (400) : photo non acceptée, ou modèle d'image inadapté (IA_IMAGE_MODEL).";
            case 401, 403 -> "Clé d'édition d'image refusée (" + statut + ") : vérifiez IA_IMAGE_API_KEY et que l'API Gemini est activée pour cette clé.";
            case 404 -> "Modèle d'édition d'image introuvable (404) : vérifiez la variable IA_IMAGE_MODEL.";
            case 429 -> "Quota du service d'édition d'image atteint (429) : réessayez plus tard ou activez la facturation sur le compte Google.";
            default -> statut >= 500 ? "Le service d'édition d'image est momentanément en panne (" + statut + "). Réessayez dans un instant."
                    : "Le service d'édition d'image a refusé la demande (" + statut + ").";
        };
    }

    private static Map<String, Object> image(byte[] data, String mime) {
        return Map.of("inline_data", Map.of("mime_type", mime, "data", Base64.getEncoder().encodeToString(data)));
    }

    private static String prompt(String piece, String categorie, String vehicule, boolean avecPhotoKit) {
        return "You are a photorealistic automotive image editor. The FIRST image is the customer's car"
                + (vehicule == null || vehicule.isBlank() ? "" : " (" + vehicule + ")") + ". "
                + (avecPhotoKit ? "The SECOND image shows the aftermarket part to install: " : "Install this aftermarket part: ")
                + "\"" + piece + "\" (category: " + categorie + "). "
                + "Edit the first image so that this part is fitted on the car, adapting its shape, perspective, lighting and reflections. "
                + "Keep everything else identical: same car, angle, colour, wheels (unless the part replaces them), background and framing. "
                + "Do not add text, logos or watermarks. Return only the edited image.";
    }

    private BufferedImage extraireImage(JsonNode r) {
        if (r != null) {
            for (JsonNode p : r.path("candidates").path(0).path("content").path("parts")) {
                JsonNode d = p.has("inlineData") ? p.get("inlineData") : p.get("inline_data");
                if (d != null && d.hasNonNull("data")) {
                    try {
                        BufferedImage img = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(d.get("data").asText())));
                        if (img != null) return StockageImageService.normaliser(img, 1600);
                    } catch (IOException | IllegalArgumentException e) {
                        log.warn("Image renvoyée illisible");
                    }
                }
            }
        }
        throw new IAIndisponibleException("L'IA n'a pas renvoyé d'image (demande refusée ou contenu non supporté). Essayez une autre photo ou un autre kit.");
    }
}
