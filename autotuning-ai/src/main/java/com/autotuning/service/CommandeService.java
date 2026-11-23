package com.autotuning.service;

import com.autotuning.model.*;
import java.math.BigDecimal;
import java.util.List;

public interface CommandeService {

    /** Diagramme de séquence « passer une commande » : vérifier le stock, enregistrer commande + lignes, vider le panier. Paiement simulé. */
    Commande creerCommande(String email);

    List<Commande> historique(String email);

    Commande detailClient(String email, Long id);

    List<Commande> toutes();

    List<Commande> recentes();

    Commande parId(Long id);

    /** Changement de statut par l'administrateur ; une annulation remet les articles en stock. */
    void changerStatut(Long id, StatutCommande nouveau);

    long total();

    BigDecimal chiffreAffaires();

    List<Object[]> meilleuresVentes();
}
