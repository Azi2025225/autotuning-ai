package com.autotuning.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicules")
@Getter @Setter @NoArgsConstructor
public class Vehicule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "utilisateur_id") private Utilisateur utilisateur;
    @Column(nullable = false, length = 60) private String marque;
    @Column(nullable = false, length = 80) private String modele;
    @Column(nullable = false) private int annee;
    @Column(nullable = false, length = 80) private String motorisation;
    @Column(nullable = false, length = 30) private String carburant;
    @Column(name = "puissance_ch", nullable = false) private int puissanceCh;

    public String getLibelle() { return marque + " " + modele + " (" + annee + ") – " + motorisation + " " + carburant + ", " + puissanceCh + " ch"; }
}
