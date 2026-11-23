package com.autotuning.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MoteurRefForm {
    private Long id;
    @NotBlank(message = "La marque est obligatoire") @Size(max = 60) private String marque;
    @NotBlank(message = "Le modèle est obligatoire") @Size(max = 100) private String modele;
    @NotBlank(message = "La motorisation est obligatoire") @Size(max = 120) private String motorisation;
    @NotBlank(message = "Le carburant est obligatoire") @Pattern(regexp = "Diesel|Essence|Hybride|GPL", message = "Carburant invalide") private String carburant;
    @NotNull @Min(1980) @Max(2035) private Integer anneeDebut;
    @NotNull @Min(1980) @Max(2035) private Integer anneeFin;
    @NotNull @Min(30) @Max(1500) private Integer puissanceOrigine;
    @NotNull @Min(50) @Max(3000) private Integer coupleOrigine;
    @NotNull @Min(1000) @Max(9000) private Integer regimeCouple;
    @NotNull @Min(1500) @Max(12000) private Integer regimePuissance;
    @NotNull @Min(2000) @Max(12000) private Integer regimeMax;
    @Min(30) @Max(2500) private Integer stage1Ch;
    @Min(50) @Max(4000) private Integer stage1Nm;
    @Min(30) @Max(2500) private Integer stage2Ch;
    @Min(50) @Max(4000) private Integer stage2Nm;
    @Min(30) @Max(2500) private Integer stage3Ch;
    @Min(50) @Max(4000) private Integer stage3Nm;
    @Size(max = 500) private String source;
    private boolean verifie;
    @Size(max = 500) private String remarque;
}
