package com.autotuning.service;

import java.awt.image.BufferedImage;
import java.util.Optional;

public interface EditionImageService {

    boolean disponible();

    BufferedImage appliquerKit(BufferedImage voiture, Optional<byte[]> photoKit, String nomPiece, String categorie, String vehicule);

    /** Repeint la carrosserie de la voiture dans la couleur demandée (ex. « rouge »), le reste de l'image restant identique. */
    BufferedImage appliquerCouleur(BufferedImage voiture, String couleur, String vehicule);
}
