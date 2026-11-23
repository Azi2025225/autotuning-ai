package com.autotuning.service;

import com.autotuning.model.Categorie;
import java.util.List;

public interface CategorieService {

    List<Categorie> toutes();

    Categorie parId(Long id);

    Categorie enregistrer(Long id, String nom, String description);

    void supprimer(Long id);
}
