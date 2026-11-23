package com.autotuning.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "password_reset_requests", indexes = @Index(name = "idx_reset_request_email_date", columnList = "email_hash, date_demande"))
@Getter @Setter @NoArgsConstructor
public class PasswordResetRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "email_hash", nullable = false, length = 64)
    private String emailHash;
    @Column(name = "date_demande", nullable = false)
    private Instant dateDemande;
}
