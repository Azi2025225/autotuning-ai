package com.autotuning.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "lignes_commande")
@Getter @Setter @NoArgsConstructor
public class LigneCommande {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "commande_id") private Commande commande;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "produit_id") private Produit produit;
    @Column(nullable = false) private int quantite;
    @Column(name = "prix_unitaire", nullable = false, precision = 10, scale = 2) private BigDecimal prixUnitaire;

    public BigDecimal getSousTotal() { return prixUnitaire.multiply(BigDecimal.valueOf(quantite)); }
}
