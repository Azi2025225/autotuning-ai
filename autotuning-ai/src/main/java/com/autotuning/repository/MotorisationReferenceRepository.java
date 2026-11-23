package com.autotuning.repository;

import com.autotuning.model.MotorisationReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MotorisationReferenceRepository extends JpaRepository<MotorisationReference, Long> {
    List<MotorisationReference> findByMarqueIgnoreCase(String marque);
}
