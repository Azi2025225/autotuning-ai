package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.exception.RessourceIntrouvableException;
import com.autotuning.model.Categorie;
import com.autotuning.repository.CategorieRepository;
import com.autotuning.repository.ProduitRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategorieServiceImpl implements CategorieService {
    private final CategorieRepository repo;
    private final ProduitRepository produits;

    public CategorieServiceImpl(CategorieRepository repo, ProduitRepository produits) {
        this.repo = repo;
        this.produits = produits;
    }

    @Override
    public List<Categorie> toutes() { return repo.findAll(Sort.by("nom")); }

    @Override
    public Categorie parId(Long id) {
        return repo.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Catégorie introuvable"));
    }

    @Transactional
    @Override
    public Categorie enregistrer(Long id, String nom, String description) {
        Categorie c = id == null ? new Categorie() : parId(id);
        if ((id == null || !c.getNom().equalsIgnoreCase(nom)) && repo.existsByNomIgnoreCase(nom)) {
            throw new IllegalArgumentException("Une catégorie portant ce nom existe déjà.");
        }
        c.setNom(nom.trim());
        c.setDescription(description);
        return repo.save(c);
    }

    @Transactional
    @Override
    public void supprimer(Long id) {
        if (produits.countByCategorieId(id) > 0) {
            throw new IllegalStateException("Impossible de supprimer : des produits utilisent cette catégorie.");
        }
        repo.deleteById(id);
    }
}
