package com.autotuning.service;

import com.autotuning.model.LignePanier;
import java.math.BigDecimal;
import java.util.List;

public interface PanierService {

    List<LignePanier> lignes(String email);

    BigDecimal total(List<LignePanier> lignes);

    int nombreArticles(String email);

    void ajouter(String email, Long produitId, int quantite);

    void modifier(String email, Long ligneId, int quantite);

    void retirer(String email, Long ligneId);

    void vider(String email);
}
