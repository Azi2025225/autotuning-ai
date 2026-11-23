package com.autotuning.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "personnalisations")
@Getter @Setter @NoArgsConstructor
public class Personnalisation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "utilisateur_id") private Utilisateur utilisateur;
    @Column(name = "image_originale", nullable = false) private String imageOriginale;
    @Column(name = "image_resultat", nullable = false) private String imageResultat;
    @Column(nullable = false, length = 60) private String modification;
    @Column(name = "date_creation", nullable = false) private LocalDateTime dateCreation = LocalDateTime.now();
    /** Texte affiché : ex. « Kit carrosserie complet sur BMW M4 ». */
    @Column(length = 255) private String libelle;
    /** Identifiant du produit appliqué (sans clé étrangère : le produit peut être supprimé plus tard). */
    @Column(name = "produit_id") private Long produitId;
    /** true si l'image a été générée par l'IA, false pour la simulation graphique de démonstration. */
    @Column(nullable = false) private boolean ia;
    @Column(length = 300) private String avertissement;
}
