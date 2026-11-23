package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.model.Commande;
import com.autotuning.model.LigneCommande;
import com.autotuning.model.Produit;
import com.autotuning.model.StatutCommande;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class StatistiquesServiceImpl implements StatistiquesService {
    private final ProduitService produits;
    private final CommandeService commandes;
    private final UtilisateurService utilisateurs;
    private final com.autotuning.repository.ConsultationIARepository consultations;
    private final com.autotuning.repository.PersonnalisationRepository personnalisations;

    public StatistiquesServiceImpl(ProduitService produits, CommandeService commandes, UtilisateurService utilisateurs,
                               com.autotuning.repository.ConsultationIARepository consultations,
                               com.autotuning.repository.PersonnalisationRepository personnalisations) {
        this.produits = produits; this.commandes = commandes; this.utilisateurs = utilisateurs;
        this.consultations = consultations; this.personnalisations = personnalisations;
    }

    @Override
    public long nbConsultations() { return consultations.count(); }
    @Override
    public long nbPersonnalisations() { return personnalisations.count(); }
    @Override
    public List<Produit> stockFaible() {
        return produits.tous().stream().filter(p -> p.getStock() <= 3).sorted(Comparator.comparingInt(Produit::getStock)).toList();
    }

    @Override
    public long nbProduits() { return produits.total(); }
    @Override
    public long nbCommandes() { return commandes.total(); }
    @Override
    public long nbClients() { return utilisateurs.total(); }
    @Override
    public BigDecimal chiffreAffaires() { return commandes.chiffreAffaires(); }
    @Override
    public List<Object[]> meilleuresVentes() { return commandes.meilleuresVentes().stream().limit(5).toList(); }

    /** Toutes les statistiques de la page détaillée, calculées à partir des commandes non annulées. */
    @Override
    public Map<String, Object> detail(int jours) {
        List<Commande> toutes = commandes.toutes();
        List<Commande> valides = toutes.stream().filter(c -> c.getStatut() != StatutCommande.ANNULEE).toList();

        // chiffre d'affaires par jour (jours sans vente = 0)
        LocalDate fin = LocalDate.now();
        Map<LocalDate, BigDecimal> parJour = new LinkedHashMap<>();
        for (int i = jours - 1; i >= 0; i--) parJour.put(fin.minusDays(i), BigDecimal.ZERO);
        for (Commande c : valides) parJour.computeIfPresent(c.getDateCommande().toLocalDate(), (d, v) -> v.add(c.getTotal()));
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM");

        // commandes par statut
        Map<String, Long> parStatut = new LinkedHashMap<>();
        for (StatutCommande s : StatutCommande.values()) parStatut.put(s.getLibelle(), 0L);
        for (Commande c : toutes) parStatut.merge(c.getStatut().getLibelle(), 1L, Long::sum);

        // chiffre d'affaires par catégorie et quantités par produit
        Map<String, BigDecimal> parCategorie = new TreeMap<>();
        Map<String, Integer> parProduit = new HashMap<>();
        for (Commande c : valides) {
            for (LigneCommande l : c.getLignes()) {
                parCategorie.merge(l.getProduit().getCategorie().getNom(), l.getSousTotal(), BigDecimal::add);
                parProduit.merge(l.getProduit().getNom(), l.getQuantite(), Integer::sum);
            }
        }
        List<Map.Entry<String, Integer>> top = parProduit.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(8).toList();

        BigDecimal ca = valides.stream().map(Commande::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal panierMoyen = valides.isEmpty() ? BigDecimal.ZERO : ca.divide(BigDecimal.valueOf(valides.size()), 2, RoundingMode.HALF_UP);
        List<Produit> stockFaible = produits.tous().stream().filter(p -> p.getStock() <= 3)
                .sorted(Comparator.comparingInt(Produit::getStock)).toList();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jours", jours);
        m.put("labelsJour", parJour.keySet().stream().map(d -> d.format(f)).toList());
        m.put("valeursJour", new ArrayList<>(parJour.values()));
        m.put("labelsStatut", new ArrayList<>(parStatut.keySet()));
        m.put("valeursStatut", new ArrayList<>(parStatut.values()));
        m.put("labelsCategorie", new ArrayList<>(parCategorie.keySet()));
        m.put("valeursCategorie", new ArrayList<>(parCategorie.values()));
        m.put("labelsTop", top.stream().map(Map.Entry::getKey).toList());
        m.put("valeursTop", top.stream().map(Map.Entry::getValue).toList());
        m.put("ca", ca);
        m.put("nbCommandesValides", valides.size());
        m.put("panierMoyen", panierMoyen);
        m.put("stockFaible", stockFaible);
        return m;
    }
}
