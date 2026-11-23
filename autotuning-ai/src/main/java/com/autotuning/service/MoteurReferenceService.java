package com.autotuning.service;

import com.autotuning.dto.MoteurRefForm;
import com.autotuning.model.MoteurReference;
import java.io.InputStream;
import java.util.*;

public interface MoteurReferenceService {

    /** Une courbe (puissance en ch, couple en Nm) pour un état : origine, Stage 1, 2 ou 3. */
    record Serie(String nom, List<Double> puissance, List<Double> couple) { }

    record Courbes(List<Integer> regimes, List<Serie> series) { }

    /** Ligne du tableau comparatif. */
    record LigneStage(String nom, Integer ch, Integer nm, Integer gainCh, Integer gainNm, Integer gainPourcent) { }

    /** Résultat d'un import CSV : lignes ajoutées, mises à jour, ignorées (déjà présentes) et messages d'erreur par ligne. */
    record ResumeImport(int ajoutes, int misAJour, int ignores, List<String> erreurs) { }

    List<MoteurReference> tous();

    MoteurReference parId(Long id);

    void supprimer(Long id);

    MoteurReference enregistrer(MoteurRefForm f);

    MoteurRefForm versForm(MoteurReference m);

    /**
     * Cherche un moteur de référence correspondant au véhicule saisi : même marque, même carburant,
     * modèle compatible, année dans la plage et puissance d'origine à ±5 ch. Sinon : aucun chiffre n'est affiché.
     */
    Optional<MoteurReference> trouver(String marque, String modele, int annee, String carburant, int puissanceCh);

    List<LigneStage> tableau(MoteurReference m);

    /**
     * Courbes SCHÉMATIQUES construites à partir des valeurs de pointe (puissance, couple) et de leurs régimes :
     * ce ne sont pas des relevés de banc d'essai. Le couple est interpolé entre quelques points, la puissance
     * en découle (P = C × régime / 9549), puis chaque courbe est recalée sur ses valeurs de pointe.
     */
    Courbes courbes(MoteurReference m);

    /**
     * Importe des moteurs de référence depuis un CSV (séparateur « ; », « , » ou tabulation, UTF-8, première ligne = en-têtes).
     * Chaque ligne est validée comme dans le formulaire admin et doit citer sa source. Si {@code ecraser} est faux,
     * les moteurs déjà présents (même marque, modèle, motorisation et année de début) sont ignorés.
     */
    ResumeImport importerCsv(InputStream in, boolean ecraser);

    /** Exporte tous les moteurs au format CSV (« ; », UTF-8) : sert aussi de modèle pour l'import. */
    String exporterCsv();
}
