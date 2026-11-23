package com.autotuning.repository;

import com.autotuning.model.LignePanier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LignePanierRepository extends JpaRepository<LignePanier, Long> {
    List<LignePanier> findByUtilisateurIdOrderByIdAsc(Long utilisateurId);
    Optional<LignePanier> findByUtilisateurIdAndProduitId(Long utilisateurId, Long produitId);
    Optional<LignePanier> findByIdAndUtilisateurId(Long id, Long utilisateurId);
    void deleteByUtilisateurId(Long utilisateurId);
    void deleteByProduitId(Long produitId);
}
