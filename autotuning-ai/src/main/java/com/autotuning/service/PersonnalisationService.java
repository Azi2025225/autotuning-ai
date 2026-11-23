package com.autotuning.service;

import com.autotuning.model.Personnalisation;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.awt.*;
import java.util.List;

public interface PersonnalisationService {

    static final List<String> MODIFICATIONS = List.of("COULEUR_ROUGE", "COULEUR_BLEU", "COULEUR_VERT");

    static String libelle(String m) {
        return switch (m) {
            case "COULEUR_ROUGE" -> "Couleur rouge";
            case "COULEUR_BLEU" -> "Couleur bleue";
            case "COULEUR_VERT" -> "Couleur verte";
            case "KIT" -> "Kit du catalogue";
            default -> m;
        };
    }

    /**
     * @param fichier   photo du client (facultative si un modèle du catalogue est choisi)
     * @param modeleId  modèle de voiture du catalogue (facultatif si une photo est fournie)
     * @param produitId kit/pièce du catalogue à appliquer (facultatif si une couleur est choisie)
     * @param couleur   changement de couleur prédéfini (si aucun produit n'est choisi)
     */
    Personnalisation creer(String email, MultipartFile fichier, Long modeleId, Long produitId, String couleur);

    List<Personnalisation> historique(String email);

    Personnalisation parId(String email, Long id);

    Resource image(Personnalisation p, boolean apres);
}
