package com.autotuning.service;

import java.awt.image.BufferedImage;
import java.util.Optional;

public interface VerificationPhotoService {

    record Resultat(boolean voiture, String description) { }

    Optional<Resultat> analyser(BufferedImage image);
}
