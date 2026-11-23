package com.autotuning.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Voiture présentée au catalogue (photo de base) sur laquelle un client peut essayer un kit. */
@Entity
@Table(name = "modeles_voiture")
@Getter @Setter @NoArgsConstructor
public class ModeleVoiture {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 60) private String marque;
    @Column(nullable = false, length = 100) private String nom;
    @Column(name = "image_url", nullable = false) private String imageUrl;

    public String getLibelle() { return marque + " " + nom; }
}
