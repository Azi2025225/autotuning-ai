package com.autotuning.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commandes")
@Getter @Setter @NoArgsConstructor
public class Commande {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "utilisateur_id") private Utilisateur utilisateur;
    @Column(name = "date_commande", nullable = false) private LocalDateTime dateCommande = LocalDateTime.now();
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal total = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatutCommande statut = StatutCommande.EN_ATTENTE;
    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<LigneCommande> lignes = new ArrayList<>();

    public void ajouterLigne(LigneCommande l) { l.setCommande(this); lignes.add(l); }
}
