package com.autotuning.service.impl;

import com.autotuning.service.*;

import com.autotuning.config.OllamaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationPhotoServiceTest {
    private static final BufferedImage IMG = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);

    @Test
    void sansModeleVisionLaVerificationEstIgnoree() {
        OllamaProperties p = new OllamaProperties(null, null, null, null, 2);
        assertTrue(new VerificationPhotoServiceImpl(new OllamaClientImpl(p), p, new ObjectMapper()).analyser(IMG).isEmpty());
    }

    @Test
    void ollamaInjoignableNeBloquePasLeSite() {
        OllamaProperties p = new OllamaProperties("http://localhost:1", null, null, "llama3.2-vision", 2);
        assertTrue(new VerificationPhotoServiceImpl(new OllamaClientImpl(p), p, new ObjectMapper()).analyser(IMG).isEmpty());
    }
}
