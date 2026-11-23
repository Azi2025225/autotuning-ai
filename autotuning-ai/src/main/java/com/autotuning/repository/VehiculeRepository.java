package com.autotuning.repository;

import com.autotuning.model.Vehicule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehiculeRepository extends JpaRepository<Vehicule, Long> {
    List<Vehicule> findByUtilisateurIdOrderByIdDesc(Long utilisateurId);
}
