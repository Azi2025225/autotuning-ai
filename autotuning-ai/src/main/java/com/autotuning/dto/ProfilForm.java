package com.autotuning.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProfilForm {
    @NotBlank(message = "Le nom est obligatoire") @Size(max = 80) private String nom;
    @NotBlank(message = "Le prénom est obligatoire") @Size(max = 80) private String prenom;
    @Pattern(regexp = "^$|^[+0-9 ()-]{8,20}$", message = "Numéro de téléphone invalide") private String telephone;
    @Pattern(regexp = "^$|^(?=.*\\p{L})(?=.*\\d).{8,72}$", message = "Le mot de passe doit contenir entre 8 et 72 caractères, avec au moins une lettre et un chiffre")
    private String nouveauMotDePasse;
}
