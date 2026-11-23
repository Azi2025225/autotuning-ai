package com.autotuning.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Données de référence publiées par des préparateurs pour une motorisation précise.
 * Le conseiller n'affiche des chiffres QUE pour les motorisations présentes ici, avec leur source.
 * Les valeurs d'un stage non renseigné restent vides : aucune donnée n'est inventée.
 */
@Entity
@Table(name = "motorisations_reference")
@Getter @Setter @NoArgsConstructor
public class MotorisationReference {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La marque est obligatoire") @Size(max = 60)
    @Column(nullable = false, length = 60) private String marque;
    @NotBlank(message = "Le modèle est obligatoire") @Size(max = 80)
    @Column(nullable = false, length = 80) private String modele;
    /** Autres noms utilisés par les clients, séparés par des virgules (ex. « Série 3, G20 »). */
    @Size(max = 200) @Column(length = 200) private String alias;
    @NotBlank(message = "La motorisation est obligatoire") @Size(max = 120)
    @Column(nullable = false, length = 120) private String motorisation;
    @NotBlank(message = "Le carburant est obligatoire") @Size(max = 30)
    @Column(nullable = false, length = 30) private String carburant;

    @Column(name = "annee_debut") private Integer anneeDebut;
    @Column(name = "annee_fin") private Integer anneeFin;

    @NotNull(message = "La puissance d'origine est obligatoire") @Min(value = 30, message = "Minimum 30 ch")
    @Column(name = "puissance_origine", nullable = false) private Integer puissanceOrigine;
    @NotNull(message = "Le couple d'origine est obligatoire") @Min(value = 50, message = "Minimum 50 Nm")
    @Column(name = "couple_origine", nullable = false) private Integer coupleOrigine;

    @Min(30) @Column(name = "puissance_stage1") private Integer puissanceStage1;
    @Min(50) @Column(name = "couple_stage1") private Integer coupleStage1;
    @Min(30) @Column(name = "puissance_stage2") private Integer puissanceStage2;
    @Min(50) @Column(name = "couple_stage2") private Integer coupleStage2;
    @Min(30) @Column(name = "puissance_stage3") private Integer puissanceStage3;
    @Min(50) @Column(name = "couple_stage3") private Integer coupleStage3;

    /** Régimes (tr/min) servant uniquement à dessiner des courbes illustratives ; valeurs par défaut selon le carburant. */
    @Column(name = "regime_couple_min") private Integer regimeCoupleMin;
    @Column(name = "regime_couple_max") private Integer regimeCoupleMax;
    @Column(name = "regime_max") private Integer regimeMax;

    @NotBlank(message = "La source est obligatoire (nom du préparateur ou lien)") @Size(max = 500)
    @Column(nullable = false, length = 500) private String source;
    @Size(max = 300) @Column(length = 300) private String note;

    public String getLibelle() { return marque + " " + modele + " " + motorisation + " (" + puissanceOrigine + " ch)"; }

    public Integer puissance(int stage) {
        return switch (stage) { case 1 -> puissanceStage1; case 2 -> puissanceStage2; case 3 -> puissanceStage3; default -> null; };
    }

    public Integer couple(int stage) {
        return switch (stage) { case 1 -> coupleStage1; case 2 -> coupleStage2; case 3 -> coupleStage3; default -> null; };
    }
}
