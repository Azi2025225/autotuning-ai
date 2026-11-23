package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.model.AvisClient;
import com.autotuning.model.Utilisateur;
import com.autotuning.repository.AvisClientRepository;
import com.autotuning.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AvisClientServiceImpl implements AvisClientService {
    private final AvisClientRepository avis;
    private final UtilisateurRepository utilisateurs;

    public AvisClientServiceImpl(AvisClientRepository avis, UtilisateurRepository utilisateurs) {
        this.avis = avis;
        this.utilisateurs = utilisateurs;
    }

    @Transactional(readOnly = true)
    @Override
    public List<AvisClient> tous() {
        return avis.findAllByOrderByDateCreationDesc();
    }

    @Transactional(readOnly = true)
    @Override
    public Double noteMoyenne() {
        return avis.moyenneDesNotes();
    }

    @Transactional(readOnly = true)
    @Override
    public boolean aDejaDonneUnAvis(String email) {
        return avis.findByUtilisateurEmailIgnoreCase(email).isPresent();
    }

    @Transactional
    @Override
    public void ajouter(String email, int note, String commentaire) {
        if (avis.findByUtilisateurEmailIgnoreCase(email).isPresent()) {
            throw new IllegalStateException("Vous avez déjà publié un avis.");
        }
        Utilisateur utilisateur = utilisateurs.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Compte utilisateur introuvable."));
        AvisClient nouvelAvis = new AvisClient();
        nouvelAvis.setUtilisateur(utilisateur);
        nouvelAvis.setNote(note);
        nouvelAvis.setCommentaire(commentaire.trim());
        avis.save(nouvelAvis);
    }
}
