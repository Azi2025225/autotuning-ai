package com.autotuning.repository;

import com.autotuning.model.ModeleVoiture;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModeleVoitureRepository extends JpaRepository<ModeleVoiture, Long> {
}
