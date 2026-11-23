package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.exception.RessourceIntrouvableException;
import com.autotuning.exception.StockInsuffisantException;
import com.autotuning.model.*;
import com.autotuning.repository.CommandeRepository;
import com.autotuning.repository.LignePanierRepository;
import com.autotuning.repository.ProduitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CommandeServiceImpl implements CommandeService {
    private static final Logger log = LoggerFactory.getLogger(CommandeServiceImpl.class);

    private final CommandeRepository commandes;
    private final LignePanierRepository paniers;
    private final ProduitRepository produits;
    private final UtilisateurService utilisateurs;

    public CommandeServiceImpl(CommandeRepository commandes, LignePanierRepository paniers,
                           ProduitRepository produits, UtilisateurService utilisateurs) {
        this.commandes = commandes;
        this.paniers = paniers;
        this.produits = produits;
        this.utilisateurs = utilisateurs;
    }

    /** Diagramme de séquence « passer une commande » : vérifier le stock, enregistrer commande + lignes, vider le panier. Paiement simulé. */
    @Transactional
    @Override
    public Commande creerCommande(String email) {
        Utilisateur u = utilisateurs.parEmail(email);
        List<LignePanier> lignes = paniers.findByUtilisateurIdOrderByIdAsc(u.getId());
        if (lignes.isEmpty()) throw new IllegalStateException("Votre panier est vide.");

        Commande c = new Commande();
        c.setUtilisateur(u);
        BigDecimal total = BigDecimal.ZERO;
        for (LignePanier lp : lignes) {
            Produit p = produits.findByIdForUpdate(lp.getProduit().getId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Produit indisponible"));
            if (p.getStock() < lp.getQuantite()) {
                throw new StockInsuffisantException("Stock insuffisant pour « " + p.getNom() + " » (disponible : " + p.getStock() + ").");
            }
            p.setStock(p.getStock() - lp.getQuantite());
            produits.save(p);
            LigneCommande l = new LigneCommande();
            l.setProduit(p);
            l.setQuantite(lp.getQuantite());
            l.setPrixUnitaire(p.getPrix());
            c.ajouterLigne(l);
            total = total.add(l.getSousTotal());
        }
        c.setTotal(total);
        c.setStatut(StatutCommande.CONFIRMEE); // validation simulée, sans paiement réel
        Commande saved = commandes.save(c);
        paniers.deleteByUtilisateurId(u.getId());
        log.info("Commande #{} créée pour {} (total {})", saved.getId(), email, total);
        return saved;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Commande> historique(String email) {
        return commandes.findByUtilisateurIdOrderByDateCommandeDesc(utilisateurs.parEmail(email).getId());
    }

    @Transactional(readOnly = true)
    @Override
    public Commande detailClient(String email, Long id) {
        return commandes.findByIdAndUtilisateurId(id, utilisateurs.parEmail(email).getId())
                .orElseThrow(() -> new RessourceIntrouvableException("Commande introuvable"));
    }

    @Transactional(readOnly = true)
    @Override
    public List<Commande> toutes() { return commandes.findAllByOrderByDateCommandeDesc(); }

    @Transactional(readOnly = true)
    @Override
    public List<Commande> recentes() { return commandes.findTop5ByOrderByDateCommandeDesc(); }

    @Transactional(readOnly = true)
    @Override
    public Commande parId(Long id) {
        return commandes.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Commande introuvable"));
    }

    /** Changement de statut par l'administrateur ; une annulation remet les articles en stock. */
    @Transactional
    @Override
    public void changerStatut(Long id, StatutCommande nouveau) {
        Commande c = parId(id);
        if (c.getStatut() == StatutCommande.ANNULEE) {
            throw new IllegalStateException("Une commande annulée ne peut plus être modifiée.");
        }
        if (nouveau == StatutCommande.ANNULEE) {
            for (LigneCommande l : c.getLignes()) {
                Produit p = l.getProduit();
                p.setStock(p.getStock() + l.getQuantite());
                produits.save(p);
            }
        }
        c.setStatut(nouveau);
        commandes.save(c);
    }

    @Override
    public long total() { return commandes.count(); }
    @Override
    public BigDecimal chiffreAffaires() { return commandes.chiffreAffaires(StatutCommande.ANNULEE); }
    @Override
    public List<Object[]> meilleuresVentes() { return commandes.meilleuresVentes(); }
}
