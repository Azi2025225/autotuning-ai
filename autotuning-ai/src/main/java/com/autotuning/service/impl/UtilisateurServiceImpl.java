package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.dto.InscriptionForm;
import com.autotuning.dto.ProfilForm;
import com.autotuning.exception.RessourceIntrouvableException;
import com.autotuning.model.Role;
import com.autotuning.model.Fournisseur;
import com.autotuning.model.Utilisateur;
import com.autotuning.repository.UtilisateurRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurRepository repo;
    private final PasswordEncoder encoder;

    public UtilisateurServiceImpl(UtilisateurRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur u = repo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable"));
        return User.withUsername(u.getEmail()).password(u.getMotDePasse())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())))
                .disabled(!u.isActif()).accountLocked(u.estVerrouille()).build();
    }

    @Transactional
    @Override
    public Utilisateur inscrire(InscriptionForm f) {
        if (repo.existsByEmailIgnoreCase(f.getEmail().trim())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }
        Utilisateur u = new Utilisateur();
        u.setNom(f.getNom().trim());
        u.setPrenom(f.getPrenom().trim());
        u.setEmail(f.getEmail().trim().toLowerCase());
        u.setTelephone(f.getTelephone());
        u.setMotDePasse(encoder.encode(f.getMotDePasse()));
        u.setRole(Role.CLIENT);
        u.setFournisseur(Fournisseur.LOCAL);
        return repo.save(u);
    }

    @Transactional(readOnly = true)
    @Override
    public Utilisateur parEmail(String email) {
        return repo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));
    }

    @Transactional
    @Override
    public void mettreAJourProfil(String email, ProfilForm f) {
        Utilisateur u = parEmail(email);
        u.setNom(f.getNom().trim());
        u.setPrenom(f.getPrenom().trim());
        u.setTelephone(f.getTelephone());
        if (f.getNouveauMotDePasse() != null && !f.getNouveauMotDePasse().isBlank()) {
            if (!f.getNouveauMotDePasse().matches("(?s)(?=.*\\p{L})(?=.*\\d).{8,72}")) {
                throw new IllegalArgumentException("Le mot de passe doit contenir entre 8 et 72 caractères, avec au moins une lettre et un chiffre.");
            }
            u.setMotDePasse(encoder.encode(f.getNouveauMotDePasse()));
        }
        repo.save(u);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Utilisateur> clients() { return repo.findAll(); }

    @Override
    public long total() { return repo.count(); }

    @Transactional
    @Override
    public void enregistrerEchecConnexion(String email) {
        repo.findByEmailIgnoreCase(email).ifPresent(u -> {
            if (u.estVerrouille()) return;
            int tentatives = u.getEchecsConnexion() + 1;
            u.setEchecsConnexion(tentatives);
            if (tentatives >= 5) {
                u.setVerrouilleJusqua(Instant.now().plus(10, ChronoUnit.MINUTES));
                u.setEchecsConnexion(0);
            }
        });
    }

    @Transactional
    @Override
    public void reinitialiserEchecsConnexion(String email) {
        repo.findByEmailIgnoreCase(email).ifPresent(u -> {
            u.setEchecsConnexion(0);
            u.setVerrouilleJusqua(null);
        });
    }

    @Transactional
    @Override
    public void mettreAJourAcces(Long id, Role role, boolean actif, String emailAdmin) {
        List<Utilisateur> administrateursActifs = repo.verrouillerAdministrateursActifs(Role.ADMIN);
        Utilisateur cible = repo.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));
        boolean dernierAdminActif = cible.getRole() == Role.ADMIN && cible.isActif()
                && (role != Role.ADMIN || !actif)
                && administrateursActifs.size() <= 1;
        if (dernierAdminActif) {
            throw new IllegalStateException("Impossible de retirer ou désactiver le dernier administrateur actif.");
        }
        if (cible.getEmail().equalsIgnoreCase(emailAdmin) && !actif) {
            throw new IllegalStateException("Vous ne pouvez pas désactiver votre propre compte.");
        }
        cible.setRole(role);
        cible.setActif(actif);
    }
}
