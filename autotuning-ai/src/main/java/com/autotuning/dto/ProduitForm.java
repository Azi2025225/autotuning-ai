package com.autotuning.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class ProduitForm {
    private Long id;
    @NotBlank(message = "Le nom est obligatoire") @Size(max = 150) private String nom;
    @Size(max = 1500, message = "1500 caractères maximum") private String description;
    @NotNull(message = "Le prix est obligatoire") @DecimalMin(value = "0.01", message = "Le prix doit être positif") @Digits(integer = 8, fraction = 2) private BigDecimal prix;
    @Min(value = 0, message = "Le stock ne peut pas être négatif") private int stock;
    @Size(max = 255) private String imageUrl;
    @Size(max = 300) private String compatibilite;
    @Size(max = 60) private String marque;
    @NotNull(message = "La catégorie est obligatoire") private Long categorieId;
}
