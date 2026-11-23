package com.autotuning.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Données de référence d'un moteur : puissance/couple d'origine et valeurs publiées pour chaque stage.
 * Les chiffres ne sont affichés au client que s'ils existent ici, avec leur source — aucune valeur n'est inventée par l'IA.
 */
@Entity
@Table(name = "moteurs_reference")
@Getter @Setter @NoArgsConstructor
public class MoteurReference {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 60) private String marque;
    @Column(nullable = false, length = 100) private String modele;
    @Column(nullable = false, length = 120) private String motorisation;
    @Column(nullable = false, length = 30) private String carburant;
    @Column(name = "annee_debut", nullable = false) private int anneeDebut;
    @Column(name = "annee_fin", nullable = false) private int anneeFin;

    @Column(name = "puissance_origine", nullable = false) private int puissanceOrigine;
    @Column(name = "couple_origine", nullable = false) private int coupleOrigine;
    @Column(name = "regime_couple", nullable = false) private int regimeCouple;
    @Column(name = "regime_puissance", nullable = false) private int regimePuissance;
    @Column(name = "regime_max", nullable = false) private int regimeMax;

    @Column(name = "stage1_ch") private Integer stage1Ch;
    @Column(name = "stage1_nm") private Integer stage1Nm;
    @Column(name = "stage2_ch") private Integer stage2Ch;
    @Column(name = "stage2_nm") private Integer stage2Nm;
    @Column(name = "stage3_ch") private Integer stage3Ch;
    @Column(name = "stage3_nm") private Integer stage3Nm;

    @Column(length = 500) private String source;
    /** true = valeurs contrôlées (banc d'essai/fiche fiable) ; false = valeur commerciale annoncée, à confirmer. */
    @Column(nullable = false) private boolean verifie;
    @Column(length = 500) private String remarque;

    public String getLibelle() { return marque + " " + modele + " – " + motorisation + " (" + anneeDebut + "-" + anneeFin + ")"; }
}
