package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.exception.RessourceIntrouvableException;
import com.autotuning.exception.StockInsuffisantException;
import com.autotuning.model.LignePanier;
import com.autotuning.model.Produit;
import com.autotuning.model.Utilisateur;
import com.autotuning.repository.LignePanierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PanierServiceImpl implements PanierService {
    static final int QUANTITE_MAX = 20;

    private final LignePanierRepository repo;
    private final UtilisateurService utilisateurs;
    private final ProduitService produits;

    public PanierServiceImpl(LignePanierRepository repo, UtilisateurService utilisateurs, ProduitService produits) {
        this.repo = repo;
        this.utilisateurs = utilisateurs;
        this.produits = produits;
    }

    @Transactional(readOnly = true)
    @Override
    public List<LignePanier> lignes(String email) {
        return repo.findByUtilisateurIdOrderByIdAsc(utilisateurs.parEmail(email).getId());
    }

    @Override
    public BigDecimal total(List<LignePanier> lignes) {
        return lignes.stream().map(LignePanier::getSousTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    @Override
    public int nombreArticles(String email) {
        return lignes(email).stream().mapToInt(LignePanier::getQuantite).sum();
    }

    @Transactional
    @Override
    public void ajouter(String email, Long produitId, int quantite) {
        if (quantite < 1) throw new IllegalArgumentException("Quantité invalide.");
        Utilisateur u = utilisateurs.parEmail(email);
        Produit p = produits.parId(produitId);
        LignePanier l = repo.findByUtilisateurIdAndProduitId(u.getId(), p.getId()).orElseGet(() -> {
            LignePanier n = new LignePanier();
            n.setUtilisateur(u);
            n.setProduit(p);
            n.setQuantite(0);
            return n;
        });
        int nouvelle = Math.min(l.getQuantite() + quantite, QUANTITE_MAX);
        if (nouvelle > p.getStock()) {
            throw new StockInsuffisantException("Stock insuffisant pour « " + p.getNom() + " » (disponible : " + p.getStock() + ").");
        }
        l.setQuantite(nouvelle);
        repo.save(l);
    }

    @Transactional
    @Override
    public void modifier(String email, Long ligneId, int quantite) {
        Utilisateur u = utilisateurs.parEmail(email);
        LignePanier l = repo.findByIdAndUtilisateurId(ligneId, u.getId())
                .orElseThrow(() -> new RessourceIntrouvableException("Ligne de panier introuvable"));
        if (quantite < 1) { repo.delete(l); return; }
        if (quantite > QUANTITE_MAX) quantite = QUANTITE_MAX;
        if (quantite > l.getProduit().getStock()) {
            throw new StockInsuffisantException("Stock insuffisant pour « " + l.getProduit().getNom() + " » (disponible : " + l.getProduit().getStock() + ").");
        }
        l.setQuantite(quantite);
        repo.save(l);
    }

    @Transactional
    @Override
    public void retirer(String email, Long ligneId) {
        Utilisateur u = utilisateurs.parEmail(email);
        repo.findByIdAndUtilisateurId(ligneId, u.getId()).ifPresent(repo::delete);
    }

    @Transactional
    @Override
    public void vider(String email) {
        repo.deleteByUtilisateurId(utilisateurs.parEmail(email).getId());
    }
}
