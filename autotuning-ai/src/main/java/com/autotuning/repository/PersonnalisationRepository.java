package com.autotuning.repository;

import com.autotuning.model.Personnalisation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonnalisationRepository extends JpaRepository<Personnalisation, Long> {
    List<Personnalisation> findByUtilisateurIdOrderByDateCreationDesc(Long utilisateurId);
    Optional<Personnalisation> findByIdAndUtilisateurId(Long id, Long utilisateurId);
}
