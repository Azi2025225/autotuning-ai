package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.dto.MoteurForm;
import com.autotuning.exception.RessourceIntrouvableException;
import com.autotuning.model.MotorisationReference;
import com.autotuning.repository.MotorisationReferenceRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Gestion des références moteur et recherche de la référence correspondant à la saisie d'un client. */
@Service
public class MotorisationServiceImpl implements MotorisationService {
    private final MotorisationReferenceRepository repo;

    public MotorisationServiceImpl(MotorisationReferenceRepository repo) { this.repo = repo; }

    @Override
    public List<MotorisationReference> toutes() { return repo.findAll(Sort.by("marque", "modele", "puissanceOrigine")); }

    @Override
    public MotorisationReference parId(Long id) {
        return repo.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Référence introuvable"));
    }

    @Override
    public MotorisationReference enregistrer(MotorisationReference m) { return repo.save(m); }

    @Override
    public void supprimer(Long id) { repo.deleteById(id); }

    @Override
    public long total() { return repo.count(); }

    /**
     * Cherche une référence : même marque, même carburant, modèle ou alias compatible, puissance d'origine à ±5 ch,
     * année dans la période connue. En cas de plusieurs résultats, la puissance la plus proche l'emporte.
     */
    @Override
    public Optional<MotorisationReference> trouver(MoteurForm f) {
        if (f.getMarque() == null || f.getModele() == null || f.getCarburant() == null
                || f.getPuissanceCh() == null || f.getAnnee() == null) return Optional.empty();
        String modele = norm(f.getModele());
        if (modele.length() < 2) return Optional.empty();
        return repo.findByMarqueIgnoreCase(f.getMarque().trim()).stream()
                .filter(r -> r.getCarburant().equalsIgnoreCase(f.getCarburant()))
                .filter(r -> Math.abs(r.getPuissanceOrigine() - f.getPuissanceCh()) <= 5)
                .filter(r -> r.getAnneeDebut() == null || f.getAnnee() >= r.getAnneeDebut())
                .filter(r -> r.getAnneeFin() == null || f.getAnnee() <= r.getAnneeFin())
                .filter(r -> noms(r).stream().anyMatch(n -> n.length() >= 2 && (n.contains(modele) || modele.contains(n))))
                .min(Comparator.comparingInt(r -> Math.abs(r.getPuissanceOrigine() - f.getPuissanceCh())));
    }

    private static List<String> noms(MotorisationReference r) {
        java.util.ArrayList<String> l = new java.util.ArrayList<>();
        l.add(norm(r.getModele()));
        if (r.getAlias() != null) for (String a : r.getAlias().split(",")) l.add(norm(a));
        return l;
    }

    static String norm(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
