package com.autotuning.repository;

import com.autotuning.model.MoteurReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MoteurReferenceRepository extends JpaRepository<MoteurReference, Long> {
    List<MoteurReference> findByMarqueIgnoreCase(String marque);

    Optional<MoteurReference> findFirstByMarqueIgnoreCaseAndModeleIgnoreCaseAndMotorisationIgnoreCaseAndAnneeDebut(
            String marque, String modele, String motorisation, int anneeDebut);
}
