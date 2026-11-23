package com.autotuning.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "utilisateurs")
@Getter @Setter @NoArgsConstructor
public class Utilisateur {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80) private String nom;
    @Column(nullable = false, length = 80) private String prenom;
    @Column(nullable = false, unique = true, length = 150) private String email;
    @Column(name = "mot_de_passe", nullable = false) private String motDePasse;
    @Column(length = 30) private String telephone;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Role role = Role.CLIENT;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'LOCAL'") private Fournisseur fournisseur = Fournisseur.LOCAL;
    @Column(name = "google_id", unique = true) private String googleId;
    @Column(nullable = false, columnDefinition = "boolean default true") private boolean actif = true;
    @Column(nullable = false, columnDefinition = "int default 0") private int echecsConnexion = 0;
    private java.time.Instant verrouilleJusqua;

    @OneToMany(mappedBy = "utilisateur") private List<Commande> commandes = new ArrayList<>();
    @OneToMany(mappedBy = "utilisateur") private List<Vehicule> vehicules = new ArrayList<>();

    public String getNomComplet() { return prenom + " " + nom; }

    public boolean estVerrouille() {
        return verrouilleJusqua != null && verrouilleJusqua.isAfter(java.time.Instant.now());
    }
}
