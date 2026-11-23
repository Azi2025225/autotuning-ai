package com.autotuning.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MoteurForm {
    @NotBlank(message = "La marque est obligatoire") @Size(max = 60) private String marque;
    @NotBlank(message = "Le modèle est obligatoire") @Size(max = 80) private String modele;
    @NotNull(message = "L'année est obligatoire") @Min(value = 1980, message = "Année minimale : 1980") @Max(value = 2035, message = "Année invalide") private Integer annee;
    @NotBlank(message = "Le carburant est obligatoire") @Pattern(regexp = "Diesel|Essence|Hybride|Électrique|GPL", message = "Carburant invalide") private String carburant;
    @NotBlank(message = "La motorisation est obligatoire") @Size(max = 80) private String motorisation;
    @NotNull(message = "La puissance est obligatoire") @Min(value = 30, message = "Puissance minimale : 30 ch") @Max(value = 1500, message = "Puissance maximale : 1500 ch") private Integer puissanceCh;
}
