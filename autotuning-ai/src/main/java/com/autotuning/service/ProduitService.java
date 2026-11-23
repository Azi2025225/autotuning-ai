package com.autotuning.service;

import com.autotuning.dto.ProduitForm;
import com.autotuning.model.Produit;
import java.math.BigDecimal;
import java.util.List;

public interface ProduitService {

    List<Produit> rechercher(String q, Long categorieId, String marque, BigDecimal min, BigDecimal max, String tri);

    List<Produit> populaires();

    List<Produit> tous();

    long total();

    Produit parId(Long id);

    Produit enregistrer(ProduitForm f);

    void majStock(Long id, int stock);

    void supprimer(Long id);
}
