package com.autotuning.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor
public class Categorie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100) private String nom;
    @Column(length = 500) private String description;
    @OneToMany(mappedBy = "categorie") private List<Produit> produits = new ArrayList<>();

    public Categorie(String nom, String description) { this.nom = nom; this.description = description; }
}
