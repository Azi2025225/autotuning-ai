package com.autotuning.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "lignes_panier", uniqueConstraints = @UniqueConstraint(columnNames = {"utilisateur_id", "produit_id"}))
@Getter @Setter @NoArgsConstructor
public class LignePanier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "utilisateur_id") private Utilisateur utilisateur;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "produit_id") private Produit produit;
    @Column(nullable = false) private int quantite;

    public BigDecimal getSousTotal() { return produit.getPrix().multiply(BigDecimal.valueOf(quantite)); }
}
