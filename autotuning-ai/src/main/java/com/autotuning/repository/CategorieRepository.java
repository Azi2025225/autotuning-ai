package com.autotuning.repository;

import com.autotuning.model.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategorieRepository extends JpaRepository<Categorie, Long> {
    boolean existsByNomIgnoreCase(String nom);
}
