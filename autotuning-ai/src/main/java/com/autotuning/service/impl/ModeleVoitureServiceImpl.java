package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.exception.RessourceIntrouvableException;
import com.autotuning.model.ModeleVoiture;
import com.autotuning.repository.ModeleVoitureRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModeleVoitureServiceImpl implements ModeleVoitureService {
    private final ModeleVoitureRepository repo;

    public ModeleVoitureServiceImpl(ModeleVoitureRepository repo) { this.repo = repo; }

    @Override
    public List<ModeleVoiture> tous() { return repo.findAll(Sort.by("marque", "nom")); }

    @Override
    public ModeleVoiture parId(Long id) {
        return repo.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Modèle introuvable"));
    }

    @Override
    public ModeleVoiture enregistrer(String marque, String nom, String imageUrl) {
        ModeleVoiture m = new ModeleVoiture();
        m.setMarque(marque.trim());
        m.setNom(nom.trim());
        m.setImageUrl(imageUrl);
        return repo.save(m);
    }

    @Override
    public void supprimer(Long id) { repo.deleteById(id); }
}
