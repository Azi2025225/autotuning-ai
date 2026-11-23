package com.autotuning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Réponse structurée du conseiller moteur (Stage 1 / 2 / 3). Les textes viennent de l'IA ; les chiffres
 * (puissance, couple, gains, courbes) viennent uniquement de la référence publiée, jamais de l'IA.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReponseIA(List<Stage> stages, String estimation, String avertissement, Reference reference, Courbes courbes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stage(int niveau, String titre, String description, List<String> modifications,
                        List<String> avantages, List<String> limites, List<String> risques, String homologation,
                        Integer puissance, Integer couple, Integer gainPuissance, Integer gainCouple) {
        public List<String> modifications() { return modifications == null ? List.of() : modifications; }
        public List<String> avantages() { return avantages == null ? List.of() : avantages; }
        public List<String> limites() { return limites == null ? List.of() : limites; }
        public List<String> risques() { return risques == null ? List.of() : risques; }

        public Stage avecValeurs(Integer puissance, Integer couple, Integer gainPuissance, Integer gainCouple) {
            return new Stage(niveau, titre, description, modifications, avantages, limites, risques, homologation,
                    puissance, couple, gainPuissance, gainCouple);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Reference(String libelle, String source, String note, int puissanceOrigine, int coupleOrigine) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Courbes(List<Integer> regimes, List<Serie> series) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Serie(String nom, List<Double> puissance, List<Double> couple) { }
}
