package com.autotuning.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "produits")
@Getter @Setter @NoArgsConstructor
public class Produit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150) private String nom;
    @Column(length = 1500) private String description;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal prix;
    @Column(nullable = false) private int stock;
    @Column(name = "image_url") private String imageUrl;
    /** Compatibilité véhicule (optionnelle), ex. « BMW Série 3 (F30), Série 4 (F32) ». */
    @Column(length = 300) private String compatibilite;
    private String marque;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "categorie_id") private Categorie categorie;

    public boolean isDisponible() { return stock > 0; }
}
