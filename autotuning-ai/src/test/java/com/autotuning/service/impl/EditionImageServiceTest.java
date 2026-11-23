package com.autotuning.service.impl;

import com.autotuning.service.*;

import com.autotuning.config.ImageIAProperties;
import com.autotuning.exception.IAIndisponibleException;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EditionImageServiceTest {
    @Test
    void sansCleLEditionIAEstIndisponible() {
        EditionImageService s = new EditionImageServiceImpl(new ImageIAProperties("", null, 5));
        assertFalse(s.disponible());
        assertThrows(IAIndisponibleException.class,
                () -> s.appliquerKit(new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB), Optional.empty(), "Spoiler", "Spoilers", "BMW M4"));
    }

    @Test
    void sansCleLaCouleurParIAEstIndisponibleAussi() {
        EditionImageService s = new EditionImageServiceImpl(new ImageIAProperties("", null, 5));
        assertThrows(IAIndisponibleException.class,
                () -> s.appliquerCouleur(new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB), "red", "BMW M4"));
    }

    @Test
    void lesErreursHttpDonnentUneConsigneAdaptee() {
        assertTrue(EditionImageServiceImpl.messagePourStatut(429).contains("Quota"));
        assertTrue(EditionImageServiceImpl.messagePourStatut(403).contains("IA_IMAGE_API_KEY"));
        assertTrue(EditionImageServiceImpl.messagePourStatut(404).contains("IA_IMAGE_MODEL"));
        assertTrue(EditionImageServiceImpl.messagePourStatut(503).contains("panne"));
    }
}
