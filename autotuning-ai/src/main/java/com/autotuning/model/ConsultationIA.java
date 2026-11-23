package com.autotuning.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "consultations_ia")
@Getter @Setter @NoArgsConstructor
public class ConsultationIA {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "utilisateur_id") private Utilisateur utilisateur;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "vehicule_id") private Vehicule vehicule;
    @Lob @Column(nullable = false, columnDefinition = "TEXT") private String demande;
    @Lob @Column(nullable = false, columnDefinition = "TEXT") private String reponse;
    @Column(name = "date_consultation", nullable = false) private LocalDateTime dateConsultation = LocalDateTime.now();
}
