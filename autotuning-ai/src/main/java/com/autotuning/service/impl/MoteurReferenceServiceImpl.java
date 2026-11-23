package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.dto.MoteurRefForm;
import com.autotuning.exception.RessourceIntrouvableException;
import com.autotuning.model.MoteurReference;
import com.autotuning.repository.MoteurReferenceRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

@Service
public class MoteurReferenceServiceImpl implements MoteurReferenceService {
    /** Une courbe (puissance en ch, couple en Nm) pour un état : origine, Stage 1, 2 ou 3. */
    /** Ligne du tableau comparatif. */

    private final MoteurReferenceRepository repo;

    public MoteurReferenceServiceImpl(MoteurReferenceRepository repo) { this.repo = repo; }

    @Override
    public List<MoteurReference> tous() { return repo.findAll(Sort.by("marque", "modele", "anneeDebut")); }

    @Override
    public MoteurReference parId(Long id) {
        return repo.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Moteur de référence introuvable"));
    }

    @Override
    public void supprimer(Long id) { repo.deleteById(id); }

    @Override
    public MoteurReference enregistrer(MoteurRefForm f) {
        if (f.getAnneeFin() < f.getAnneeDebut()) throw new IllegalArgumentException("L'année de fin doit être postérieure à l'année de début.");
        if (f.getRegimeCouple() >= f.getRegimePuissance() || f.getRegimePuissance() >= f.getRegimeMax()) {
            throw new IllegalArgumentException("Les régimes doivent respecter : régime du couple < régime de la puissance < régime maximal.");
        }
        for (int s = 1; s <= 3; s++) {
            Integer ch = s == 1 ? f.getStage1Ch() : s == 2 ? f.getStage2Ch() : f.getStage3Ch();
            Integer nm = s == 1 ? f.getStage1Nm() : s == 2 ? f.getStage2Nm() : f.getStage3Nm();
            if ((ch == null) != (nm == null)) throw new IllegalArgumentException("Stage " + s + " : renseignez la puissance ET le couple, ou aucun des deux.");
        }
        MoteurReference m = f.getId() == null ? new MoteurReference() : parId(f.getId());
        m.setMarque(f.getMarque().trim()); m.setModele(f.getModele().trim()); m.setMotorisation(f.getMotorisation().trim());
        m.setCarburant(f.getCarburant()); m.setAnneeDebut(f.getAnneeDebut()); m.setAnneeFin(f.getAnneeFin());
        m.setPuissanceOrigine(f.getPuissanceOrigine()); m.setCoupleOrigine(f.getCoupleOrigine());
        m.setRegimeCouple(f.getRegimeCouple()); m.setRegimePuissance(f.getRegimePuissance()); m.setRegimeMax(f.getRegimeMax());
        m.setStage1Ch(f.getStage1Ch()); m.setStage1Nm(f.getStage1Nm());
        m.setStage2Ch(f.getStage2Ch()); m.setStage2Nm(f.getStage2Nm());
        m.setStage3Ch(f.getStage3Ch()); m.setStage3Nm(f.getStage3Nm());
        m.setSource(f.getSource()); m.setVerifie(f.isVerifie()); m.setRemarque(f.getRemarque());
        return repo.save(m);
    }

    @Override
    public MoteurRefForm versForm(MoteurReference m) {
        MoteurRefForm f = new MoteurRefForm();
        f.setId(m.getId()); f.setMarque(m.getMarque()); f.setModele(m.getModele()); f.setMotorisation(m.getMotorisation());
        f.setCarburant(m.getCarburant()); f.setAnneeDebut(m.getAnneeDebut()); f.setAnneeFin(m.getAnneeFin());
        f.setPuissanceOrigine(m.getPuissanceOrigine()); f.setCoupleOrigine(m.getCoupleOrigine());
        f.setRegimeCouple(m.getRegimeCouple()); f.setRegimePuissance(m.getRegimePuissance()); f.setRegimeMax(m.getRegimeMax());
        f.setStage1Ch(m.getStage1Ch()); f.setStage1Nm(m.getStage1Nm()); f.setStage2Ch(m.getStage2Ch()); f.setStage2Nm(m.getStage2Nm());
        f.setStage3Ch(m.getStage3Ch()); f.setStage3Nm(m.getStage3Nm());
        f.setSource(m.getSource()); f.setVerifie(m.isVerifie()); f.setRemarque(m.getRemarque());
        return f;
    }

    /**
     * Cherche un moteur de référence correspondant au véhicule saisi : même marque, même carburant,
     * modèle compatible, année dans la plage et puissance d'origine à ±5 ch. Sinon : aucun chiffre n'est affiché.
     */
    @Override
    public Optional<MoteurReference> trouver(String marque, String modele, int annee, String carburant, int puissanceCh) {
        String mod = norm(modele);
        return repo.findByMarqueIgnoreCase(marque.trim()).stream()
                .filter(m -> m.getCarburant().equalsIgnoreCase(carburant))
                .filter(m -> annee >= m.getAnneeDebut() && annee <= m.getAnneeFin())
                .filter(m -> Math.abs(m.getPuissanceOrigine() - puissanceCh) <= 5)
                .filter(m -> { String r = norm(m.getModele()); return !mod.isEmpty() && (mod.contains(r) || r.contains(mod)); })
                .min(Comparator.comparingInt(m -> Math.abs(m.getPuissanceOrigine() - puissanceCh)));
    }

    private static String norm(String s) { return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]", ""); }

    // ------------------------------------------------------------------ tableau et courbes

    @Override
    public List<LigneStage> tableau(MoteurReference m) {
        List<LigneStage> l = new ArrayList<>();
        l.add(new LigneStage("Origine", m.getPuissanceOrigine(), m.getCoupleOrigine(), null, null, null));
        ajouter(l, "Stage 1", m, m.getStage1Ch(), m.getStage1Nm());
        ajouter(l, "Stage 2", m, m.getStage2Ch(), m.getStage2Nm());
        ajouter(l, "Stage 3", m, m.getStage3Ch(), m.getStage3Nm());
        return l;
    }

    private static void ajouter(List<LigneStage> l, String nom, MoteurReference m, Integer ch, Integer nm) {
        if (ch == null || nm == null) return;
        l.add(new LigneStage(nom, ch, nm, ch - m.getPuissanceOrigine(), nm - m.getCoupleOrigine(),
                Math.round(100f * (ch - m.getPuissanceOrigine()) / m.getPuissanceOrigine())));
    }

    /**
     * Courbes SCHÉMATIQUES construites à partir des valeurs de pointe (puissance, couple) et de leurs régimes :
     * ce ne sont pas des relevés de banc d'essai. Le couple est interpolé entre quelques points, la puissance
     * en découle (P = C × régime / 9549), puis chaque courbe est recalée sur ses valeurs de pointe.
     */
    @Override
    public Courbes courbes(MoteurReference m) {
        List<Integer> rpm = new ArrayList<>();
        for (int r = 1000; r <= m.getRegimeMax(); r += 250) rpm.add(r);
        List<Serie> series = new ArrayList<>();
        series.add(serie("Origine", m, m.getPuissanceOrigine(), m.getCoupleOrigine(), rpm));
        if (m.getStage1Ch() != null && m.getStage1Nm() != null) series.add(serie("Stage 1", m, m.getStage1Ch(), m.getStage1Nm(), rpm));
        if (m.getStage2Ch() != null && m.getStage2Nm() != null) series.add(serie("Stage 2", m, m.getStage2Ch(), m.getStage2Nm(), rpm));
        if (m.getStage3Ch() != null && m.getStage3Nm() != null) series.add(serie("Stage 3", m, m.getStage3Ch(), m.getStage3Nm(), rpm));
        return new Courbes(rpm, series);
    }

    private static Serie serie(String nom, MoteurReference m, int ch, int nm, List<Integer> rpm) {
        double rC = Math.max(1200, Math.min(m.getRegimeCouple(), m.getRegimePuissance() - 300));
        double rP = m.getRegimePuissance(), rMax = m.getRegimeMax();
        double kw = ch * 0.7355;
        double tP = Math.min(nm, 9549 * kw / rP);
        double[][] pts = {{1000, 0.5 * nm}, {rC, nm}, {rP, tP}, {rMax, 0.72 * tP}};

        double[] t = new double[rpm.size()], p = new double[rpm.size()];
        double maxT = 0, maxP = 0;
        for (int i = 0; i < rpm.size(); i++) {
            double r = rpm.get(i);
            t[i] = interpoler(pts, r);
            p[i] = t[i] * r / 9549 / 0.7355;
            maxT = Math.max(maxT, t[i]);
            maxP = Math.max(maxP, p[i]);
        }
        List<Double> puiss = new ArrayList<>(), couple = new ArrayList<>();
        for (int i = 0; i < rpm.size(); i++) {
            couple.add(Math.round(t[i] * nm / maxT * 10) / 10.0);
            puiss.add(Math.round(p[i] * ch / maxP * 10) / 10.0);
        }
        return new Serie(nom, puiss, couple);
    }

    private static double interpoler(double[][] pts, double r) {
        if (r <= pts[0][0]) return pts[0][1];
        for (int i = 0; i < pts.length - 1; i++) {
            if (r <= pts[i + 1][0]) {
                double u = (r - pts[i][0]) / (pts[i + 1][0] - pts[i][0]);
                double e = (1 - Math.cos(Math.PI * u)) / 2; // transition douce
                return pts[i][1] + (pts[i + 1][1] - pts[i][1]) * e;
            }
        }
        return pts[pts.length - 1][1];
    }

    // ------------------------------------------------------------------ import / export CSV

    private static final int LIGNES_MAX = 2000;
    private static final long OCTETS_MAX = 2L * 1024 * 1024;
    private static final String[] ENTETE = {"marque", "modele", "motorisation", "carburant", "annee_debut", "annee_fin",
            "puissance_origine", "couple_origine", "regime_couple", "regime_puissance", "regime_max",
            "stage1_ch", "stage1_nm", "stage2_ch", "stage2_nm", "stage3_ch", "stage3_nm", "source", "verifie", "remarque"};
    private static final List<String> OBLIGATOIRES = List.of("marque", "modele", "motorisation", "carburant", "anneedebut", "anneefin",
            "puissanceorigine", "coupleorigine", "source");

    private static Validator validateur;

    private static synchronized Validator validateur() {
        if (validateur == null) validateur = Validation.buildDefaultValidatorFactory().getValidator();
        return validateur;
    }

    @Override
    public ResumeImport importerCsv(InputStream in, boolean ecraser) {
        String texte;
        try {
            byte[] octets = in.readNBytes((int) OCTETS_MAX + 1);
            if (octets.length > OCTETS_MAX) throw new IllegalArgumentException("Fichier trop volumineux (2 Mo maximum).");
            texte = new String(octets, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Lecture du fichier impossible.");
        }
        if (texte.startsWith("﻿")) texte = texte.substring(1);
        if (texte.indexOf('\u0000') >= 0) throw new IllegalArgumentException("Ce fichier n'est pas un CSV texte.");

        List<List<String>> lignes = lireCsv(texte, detecterSeparateur(texte));
        if (lignes.isEmpty()) throw new IllegalArgumentException("Le fichier est vide.");
        if (lignes.size() - 1 > LIGNES_MAX) throw new IllegalArgumentException("Trop de lignes (" + LIGNES_MAX + " maximum).");

        Map<String, Integer> col = new HashMap<>();
        List<String> entete = lignes.get(0);
        for (int i = 0; i < entete.size(); i++) col.put(cle(entete.get(i)), i);
        List<String> manquantes = OBLIGATOIRES.stream().filter(c -> !col.containsKey(c)).toList();
        if (!manquantes.isEmpty()) {
            throw new IllegalArgumentException("Colonnes manquantes dans l'en-tête : " + String.join(", ", manquantes)
                    + ". Téléchargez le modèle CSV pour voir le format attendu.");
        }

        int ajoutes = 0, misAJour = 0, ignores = 0;
        List<String> erreurs = new ArrayList<>();
        for (int n = 1; n < lignes.size(); n++) {
            List<String> l = lignes.get(n);
            if (l.stream().allMatch(String::isBlank)) continue;
            String ref = "Ligne " + (n + 1);
            try {
                MoteurRefForm f = versFormulaire(l, col);
                Optional<MoteurReference> existant = repo.findFirstByMarqueIgnoreCaseAndModeleIgnoreCaseAndMotorisationIgnoreCaseAndAnneeDebut(
                        f.getMarque().trim(), f.getModele().trim(), f.getMotorisation().trim(), f.getAnneeDebut());
                if (existant.isPresent() && !ecraser) { ignores++; continue; }
                existant.ifPresent(e -> f.setId(e.getId()));
                Set<ConstraintViolation<MoteurRefForm>> v = validateur().validate(f);
                if (!v.isEmpty()) {
                    ConstraintViolation<MoteurRefForm> premier = v.stream().min(Comparator.comparing(x -> x.getPropertyPath().toString())).get();
                    throw new IllegalArgumentException(premier.getPropertyPath() + " : " + premier.getMessage());
                }
                enregistrer(f);
                if (existant.isPresent()) misAJour++; else ajoutes++;
            } catch (IllegalArgumentException e) {
                erreurs.add(ref + " : " + e.getMessage());
            }
        }
        return new ResumeImport(ajoutes, misAJour, ignores, erreurs);
    }

    private MoteurRefForm versFormulaire(List<String> l, Map<String, Integer> col) {
        MoteurRefForm f = new MoteurRefForm();
        f.setMarque(texte(l, col, "marque"));
        f.setModele(texte(l, col, "modele"));
        f.setMotorisation(texte(l, col, "motorisation"));
        String carburant = texte(l, col, "carburant");
        f.setCarburant(carburant == null ? null : carburant.substring(0, 1).toUpperCase() + carburant.substring(1).toLowerCase());
        f.setAnneeDebut(entier(l, col, "anneedebut"));
        f.setAnneeFin(entier(l, col, "anneefin"));
        f.setPuissanceOrigine(entier(l, col, "puissanceorigine"));
        f.setCoupleOrigine(entier(l, col, "coupleorigine"));
        f.setStage1Ch(entier(l, col, "stage1ch")); f.setStage1Nm(entier(l, col, "stage1nm"));
        f.setStage2Ch(entier(l, col, "stage2ch")); f.setStage2Nm(entier(l, col, "stage2nm"));
        f.setStage3Ch(entier(l, col, "stage3ch")); f.setStage3Nm(entier(l, col, "stage3nm"));

        String source = texte(l, col, "source");
        if (source == null) throw new IllegalArgumentException("la source est obligatoire (aucune valeur sans source).");
        f.setSource(source.length() > 500 ? source.substring(0, 500) : source);
        String verifie = texte(l, col, "verifie");
        f.setVerifie(verifie != null && verifie.toLowerCase().matches("1|true|vrai|oui|yes|x"));
        String remarque = texte(l, col, "remarque");

        Integer rc = entier(l, col, "regimecouple"), rp = entier(l, col, "regimepuissance"), rm = entier(l, col, "regimemax");
        if (rc == null || rp == null || rm == null) {
            boolean diesel = "Diesel".equals(f.getCarburant());
            rc = diesel ? 1750 : 2000; rp = diesel ? 3750 : 5500; rm = diesel ? 4500 : 6500;
            String note = "Régimes (tr/min) estimés par défaut : la forme des courbes est schématique.";
            remarque = remarque == null ? note : remarque + " " + note;
        }
        f.setRegimeCouple(rc); f.setRegimePuissance(rp); f.setRegimeMax(rm);
        f.setRemarque(remarque != null && remarque.length() > 500 ? remarque.substring(0, 500) : remarque);
        return f;
    }

    private static String texte(List<String> l, Map<String, Integer> col, String nom) {
        Integer i = col.get(nom);
        if (i == null || i >= l.size()) return null;
        String v = l.get(i).trim();
        return v.isEmpty() ? null : v;
    }

    private static Integer entier(List<String> l, Map<String, Integer> col, String nom) {
        String v = texte(l, col, nom);
        if (v == null) return null;
        try {
            return Integer.parseInt(v.replace(" ", "").replace(" ", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(nom + " : « " + v + " » n'est pas un nombre entier.");
        }
    }

    private static String cle(String s) {
        return Normalizer.normalize(s == null ? "" : s.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").replaceAll("[^a-z0-9]", "");
    }

    private static char detecterSeparateur(String texte) {
        int fin = texte.indexOf('\n');
        String premiere = fin < 0 ? texte : texte.substring(0, fin);
        long pv = premiere.chars().filter(c -> c == ';').count(), v = premiere.chars().filter(c -> c == ',').count(),
                t = premiere.chars().filter(c -> c == '\t').count();
        return pv >= v && pv >= t ? ';' : (v >= t ? ',' : '\t');
    }

    /** Lecteur CSV minimal (RFC 4180) : champs entre guillemets, guillemets doublés, retours à la ligne dans un champ. */
    static List<List<String>> lireCsv(String texte, char sep) {
        List<List<String>> res = new ArrayList<>();
        List<String> ligne = new ArrayList<>();
        StringBuilder champ = new StringBuilder();
        boolean guillemets = false;
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (guillemets) {
                if (c == '"') {
                    if (i + 1 < texte.length() && texte.charAt(i + 1) == '"') { champ.append('"'); i++; }
                    else guillemets = false;
                } else champ.append(c);
            } else if (c == '"') {
                guillemets = true;
            } else if (c == sep) {
                ligne.add(champ.toString()); champ.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < texte.length() && texte.charAt(i + 1) == '\n') i++;
                ligne.add(champ.toString()); champ.setLength(0);
                res.add(ligne); ligne = new ArrayList<>();
            } else champ.append(c);
        }
        if (champ.length() > 0 || !ligne.isEmpty()) { ligne.add(champ.toString()); res.add(ligne); }
        return res;
    }

    @Override
    public String exporterCsv() {
        StringBuilder sb = new StringBuilder(String.join(";", ENTETE)).append("\r\n");
        for (MoteurReference m : tous()) {
            String[] v = {m.getMarque(), m.getModele(), m.getMotorisation(), m.getCarburant(), String.valueOf(m.getAnneeDebut()),
                    String.valueOf(m.getAnneeFin()), String.valueOf(m.getPuissanceOrigine()), String.valueOf(m.getCoupleOrigine()),
                    String.valueOf(m.getRegimeCouple()), String.valueOf(m.getRegimePuissance()), String.valueOf(m.getRegimeMax()),
                    nb(m.getStage1Ch()), nb(m.getStage1Nm()), nb(m.getStage2Ch()), nb(m.getStage2Nm()), nb(m.getStage3Ch()), nb(m.getStage3Nm()),
                    m.getSource(), m.isVerifie() ? "1" : "0", m.getRemarque()};
            for (int i = 0; i < v.length; i++) sb.append(i == 0 ? "" : ";").append(cellule(v[i]));
            sb.append("\r\n");
        }
        return sb.toString();
    }

    private static String nb(Integer n) { return n == null ? "" : n.toString(); }

    /** Échappe une cellule CSV et neutralise l'injection de formules (=, +, -, @) à l'ouverture dans un tableur. */
    private static String cellule(String s) {
        if (s == null) return "";
        String v = s.replace("\r", " ").replace("\n", " ");
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        return v.contains(";") || v.contains("\"") ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }
}
