package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.dto.ProduitForm;
import com.autotuning.exception.RessourceIntrouvableException;
import com.autotuning.model.Produit;
import com.autotuning.repository.CommandeRepository;
import com.autotuning.repository.LignePanierRepository;
import com.autotuning.repository.ProduitRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProduitServiceImpl implements ProduitService {
    private final ProduitRepository repo;
    private final CategorieService categories;
    private final LignePanierRepository paniers;
    private final CommandeRepository commandes;

    public ProduitServiceImpl(ProduitRepository repo, CategorieService categories,
                          LignePanierRepository paniers, CommandeRepository commandes) {
        this.repo = repo;
        this.categories = categories;
        this.paniers = paniers;
        this.commandes = commandes;
    }

    @Override
    public List<Produit> rechercher(String q, Long categorieId, String marque, BigDecimal min, BigDecimal max, String tri) {
        Sort sort = switch (tri == null ? "" : tri) {
            case "prix_asc" -> Sort.by("prix").ascending();
            case "prix_desc" -> Sort.by("prix").descending();
            case "nom" -> Sort.by("nom").ascending();
            default -> Sort.by("id").ascending();
        };
        return repo.rechercher(vide(q), categorieId, vide(marque), min, max, sort);
    }

    private String vide(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    @Override
    public List<Produit> populaires() { return repo.findTop4ByStockGreaterThanOrderByIdAsc(0); }
    @Override
    public List<Produit> tous() { return repo.findAll(Sort.by("id")); }
    @Override
    public long total() { return repo.count(); }

    @Override
    public Produit parId(Long id) {
        return repo.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable"));
    }

    @Transactional
    @Override
    public Produit enregistrer(ProduitForm f) {
        Produit p = f.getId() == null ? new Produit() : parId(f.getId());
        p.setNom(f.getNom().trim());
        p.setDescription(f.getDescription());
        p.setPrix(f.getPrix());
        p.setStock(f.getStock());
        p.setImageUrl(f.getImageUrl() == null || f.getImageUrl().isBlank() ? "/img/placeholder.svg" : f.getImageUrl().trim());
        p.setCompatibilite(f.getCompatibilite());
        p.setMarque(f.getMarque());
        p.setCategorie(categories.parId(f.getCategorieId()));
        return repo.save(p);
    }

    @Transactional
    @Override
    public void majStock(Long id, int stock) {
        Produit p = parId(id);
        p.setStock(stock);
        repo.save(p);
    }

    @Transactional
    @Override
    public void supprimer(Long id) {
        if (commandes.existsByLignesProduitId(id)) {
            throw new IllegalStateException("Ce produit figure dans des commandes : mettez son stock à 0 plutôt que de le supprimer.");
        }
        paniers.deleteByProduitId(id);
        repo.deleteById(id);
    }
}
