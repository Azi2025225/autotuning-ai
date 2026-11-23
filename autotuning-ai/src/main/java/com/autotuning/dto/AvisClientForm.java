package com.autotuning.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AvisClientForm {
    @Min(value = 1, message = "Choisissez une note entre 1 et 5 étoiles")
    @Max(value = 5, message = "Choisissez une note entre 1 et 5 étoiles")
    private int note;

    @NotBlank(message = "Votre commentaire est obligatoire")
    @Size(min = 10, max = 1000, message = "Votre avis doit contenir entre 10 et 1000 caractères")
    private String commentaire;
}
