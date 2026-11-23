package com.autotuning.service;

import com.autotuning.dto.MoteurForm;
import com.autotuning.model.MotorisationReference;
import java.util.List;
import java.util.Optional;

public interface MotorisationService {

    List<MotorisationReference> toutes();

    MotorisationReference parId(Long id);

    MotorisationReference enregistrer(MotorisationReference m);

    void supprimer(Long id);

    long total();

    /**
     * Cherche une référence : même marque, même carburant, modèle ou alias compatible, puissance d'origine à ±5 ch,
     * année dans la période connue. En cas de plusieurs résultats, la puissance la plus proche l'emporte.
     */
    Optional<MotorisationReference> trouver(MoteurForm f);
}
