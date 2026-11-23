package com.autotuning.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "avis_clients", uniqueConstraints = @UniqueConstraint(name = "uk_avis_utilisateur", columnNames = "utilisateur_id"))
@Getter @Setter @NoArgsConstructor
public class AvisClient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false)
    @Min(value = 1, message = "Choisissez une note entre 1 et 5 étoiles")
    @Max(value = 5, message = "Choisissez une note entre 1 et 5 étoiles")
    private int note;

    @NotBlank(message = "Votre commentaire est obligatoire")
    @Size(min = 10, max = 1000, message = "Votre avis doit contenir entre 10 et 1000 caractères")
    @Column(nullable = false, length = 1000)
    private String commentaire;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
