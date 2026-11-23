package com.autotuning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReinitialisationMotDePasseForm {
    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).{8,72}$", message = "Le mot de passe doit contenir entre 8 et 72 caractères, avec au moins une lettre et un chiffre")
    private String motDePasse;
    @NotBlank(message = "Veuillez confirmer le mot de passe")
    private String confirmation;
}
