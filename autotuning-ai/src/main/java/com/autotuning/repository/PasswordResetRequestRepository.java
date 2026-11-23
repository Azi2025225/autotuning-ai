package com.autotuning.repository;

import com.autotuning.model.PasswordResetRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {
    long countByEmailHashAndDateDemandeAfter(String emailHash, Instant since);
}
