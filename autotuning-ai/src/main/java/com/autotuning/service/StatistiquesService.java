package com.autotuning.service;

import com.autotuning.model.Produit;
import java.math.BigDecimal;
import java.util.*;

public interface StatistiquesService {

    long nbConsultations();

    long nbPersonnalisations();

    List<Produit> stockFaible();

    long nbProduits();

    long nbCommandes();

    long nbClients();

    BigDecimal chiffreAffaires();

    List<Object[]> meilleuresVentes();

    /** Toutes les statistiques de la page détaillée, calculées à partir des commandes non annulées. */
    Map<String, Object> detail(int jours);
}
