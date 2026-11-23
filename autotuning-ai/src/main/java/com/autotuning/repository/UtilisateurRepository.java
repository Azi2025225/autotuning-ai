package com.autotuning.repository;

import com.autotuning.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.List;
import com.autotuning.model.Role;
import com.autotuning.model.Role;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByRoleAndActifTrue(Role role);
    Optional<Utilisateur> findByGoogleId(String googleId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Utilisateur u where u.role = :role and u.actif = true")
    List<Utilisateur> verrouillerAdministrateursActifs(@Param("role") Role role);
}
