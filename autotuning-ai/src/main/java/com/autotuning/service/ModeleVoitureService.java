package com.autotuning.service;

import com.autotuning.model.ModeleVoiture;
import java.util.List;

public interface ModeleVoitureService {

    List<ModeleVoiture> tous();

    ModeleVoiture parId(Long id);

    ModeleVoiture enregistrer(String marque, String nom, String imageUrl);

    void supprimer(Long id);
}
