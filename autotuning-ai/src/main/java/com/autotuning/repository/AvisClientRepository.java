package com.autotuning.repository;

import com.autotuning.model.AvisClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AvisClientRepository extends JpaRepository<AvisClient, Long> {
    @Query("select a from AvisClient a join fetch a.utilisateur order by a.dateCreation desc")
    List<AvisClient> findAllByOrderByDateCreationDesc();
    Optional<AvisClient> findByUtilisateurEmailIgnoreCase(String email);

    @Query("select avg(a.note) from AvisClient a")
    Double moyenneDesNotes();
}
