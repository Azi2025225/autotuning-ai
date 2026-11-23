package com.autotuning.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InscriptionForm {
    @NotBlank(message = "Le nom est obligatoire") @Size(max = 80) private String nom;
    @NotBlank(message = "Le prénom est obligatoire") @Size(max = 80) private String prenom;
    @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") @Size(max = 150) private String email;
    @Pattern(regexp = "^$|^[+0-9 ()-]{8,20}$", message = "Numéro de téléphone invalide") private String telephone;
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).{8,72}$", message = "Le mot de passe doit contenir entre 8 et 72 caractères, avec au moins une lettre et un chiffre")
    private String motDePasse;
    @NotBlank(message = "Veuillez confirmer le mot de passe") private String confirmation;
}
