package com.autotuning.service;

import com.autotuning.dto.InscriptionForm;
import com.autotuning.dto.ProfilForm;
import com.autotuning.model.Role;
import com.autotuning.model.Utilisateur;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.util.List;

public interface UtilisateurService extends UserDetailsService {

    Utilisateur inscrire(InscriptionForm f);

    Utilisateur parEmail(String email);

    void mettreAJourProfil(String email, ProfilForm f);

    List<Utilisateur> clients();

    long total();

    void enregistrerEchecConnexion(String email);

    void reinitialiserEchecsConnexion(String email);

    void mettreAJourAcces(Long id, Role role, boolean actif, String emailAdmin);
}
