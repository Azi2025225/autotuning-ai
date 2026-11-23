package com.autotuning.repository;

import com.autotuning.model.ConsultationIA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultationIARepository extends JpaRepository<ConsultationIA, Long> {
    List<ConsultationIA> findByUtilisateurIdOrderByDateConsultationDesc(Long utilisateurId);
    Optional<ConsultationIA> findByIdAndUtilisateurId(Long id, Long utilisateurId);
}
