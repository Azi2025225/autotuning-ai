package com.autotuning.service.impl;

import com.autotuning.service.*;

import com.autotuning.model.MoteurReference;
import com.autotuning.repository.MoteurReferenceRepository;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MoteurReferenceServiceTest {

    private static MoteurReference gti() {
        MoteurReference m = new MoteurReference();
        m.setMarque("Volkswagen"); m.setModele("Golf 7 GTI"); m.setMotorisation("2.0 TSI"); m.setCarburant("Essence");
        m.setAnneeDebut(2012); m.setAnneeFin(2019); m.setPuissanceOrigine(220); m.setCoupleOrigine(350);
        m.setRegimeCouple(2500); m.setRegimePuissance(5500); m.setRegimeMax(6500);
        m.setStage1Ch(300); m.setStage1Nm(420);
        return m;
    }

    private MoteurReferenceService service(MoteurReference... refs) {
        MoteurReferenceRepository repo = mock(MoteurReferenceRepository.class);
        when(repo.findByMarqueIgnoreCase("Volkswagen")).thenReturn(List.of(refs));
        return new MoteurReferenceServiceImpl(repo);
    }

    @Test
    void trouveLeMoteurSeulementSiTousLesCriteresCorrespondent() {
        MoteurReferenceService s = service(gti());
        assertTrue(s.trouver("Volkswagen", "Golf 7 GTI", 2016, "Essence", 220).isPresent());
        assertTrue(s.trouver("Volkswagen", "golf7gti", 2016, "Essence", 223).isPresent(), "tolérance de 5 ch et de casse");
        assertTrue(s.trouver("Volkswagen", "Golf 7 GTI", 2016, "Diesel", 220).isEmpty(), "carburant différent");
        assertTrue(s.trouver("Volkswagen", "Golf 7 GTI", 2021, "Essence", 220).isEmpty(), "année hors plage");
        assertTrue(s.trouver("Volkswagen", "Golf 7 GTI", 2016, "Essence", 245).isEmpty(), "puissance différente : aucun chiffre");
        assertTrue(s.trouver("Volkswagen", "Polo", 2016, "Essence", 220).isEmpty(), "autre modèle");
    }

    @Test
    void tableauCalculeLesGains() {
        var t = service(gti()).tableau(gti());
        assertEquals(2, t.size());
        assertEquals(80, t.get(1).gainCh());
        assertEquals(70, t.get(1).gainNm());
        assertEquals(36, t.get(1).gainPourcent());
    }

    @Test
    void courbesRespectentLesValeursDePointe() {
        MoteurReferenceService.Courbes c = service(gti()).courbes(gti());
        assertEquals(2, c.series().size());
        for (var s : c.series()) {
            assertEquals(c.regimes().size(), s.puissance().size());
            assertEquals(c.regimes().size(), s.couple().size());
        }
        assertEquals(220.0, c.series().get(0).puissance().stream().mapToDouble(d -> d).max().orElseThrow(), 0.2);
        assertEquals(350.0, c.series().get(0).couple().stream().mapToDouble(d -> d).max().orElseThrow(), 0.2);
        assertEquals(300.0, c.series().get(1).puissance().stream().mapToDouble(d -> d).max().orElseThrow(), 0.2);
        assertEquals(420.0, c.series().get(1).couple().stream().mapToDouble(d -> d).max().orElseThrow(), 0.2);
    }

    // ------------------------------------------------------------------ import / export CSV

    private static final String ENTETE = "marque;modele;motorisation;carburant;annee_debut;annee_fin;puissance_origine;couple_origine;regime_couple;regime_puissance;regime_max;stage1_ch;stage1_nm;stage2_ch;stage2_nm;stage3_ch;stage3_nm;source;verifie;remarque\n";

    private static ByteArrayInputStream csv(String contenu) { return new ByteArrayInputStream(contenu.getBytes(StandardCharsets.UTF_8)); }

    @Test
    void importAjouteLesMoteursValidesEtAppliqueLesRegimesParDefaut() {
        MoteurReferenceRepository repo = mock(MoteurReferenceRepository.class);
        MoteurReferenceServiceImpl s = new MoteurReferenceServiceImpl(repo);
        var r = s.importerCsv(csv(ENTETE
                + "Dacia;Duster;\"1.5 dCi 105 ch; K9K\";Diesel;2010;2017;105;240;;;;130;300;;;;;https://exemple.test/duster;0;Valeurs annoncées\n"), false);
        assertEquals(1, r.ajoutes());
        assertTrue(r.erreurs().isEmpty(), r.erreurs().toString());
        org.mockito.ArgumentCaptor<MoteurReference> c = org.mockito.ArgumentCaptor.forClass(MoteurReference.class);
        verify(repo).save(c.capture());
        MoteurReference m = c.getValue();
        assertEquals("1.5 dCi 105 ch; K9K", m.getMotorisation(), "champ entre guillemets contenant le séparateur");
        assertEquals(130, m.getStage1Ch());
        assertEquals(1750, m.getRegimeCouple());
        assertTrue(m.getRemarque().contains("estimés par défaut"));
        assertFalse(m.isVerifie());
    }

    @Test
    void importRefuseUneLigneSansSourceOuInvalideSansRienEnregistrer() {
        MoteurReferenceRepository repo = mock(MoteurReferenceRepository.class);
        MoteurReferenceServiceImpl s = new MoteurReferenceServiceImpl(repo);
        var r = s.importerCsv(csv(ENTETE
                + "Dacia;Duster;1.5 dCi;Diesel;2010;2017;105;240;;;;130;300;;;;;;0;\n"
                + "Dacia;Duster;1.5 dCi;Kerosene;2010;2017;105;240;;;;;;;;;;https://x.test;0;\n"
                + "Dacia;Duster;1.5 dCi;Diesel;2010;2017;105;240;;;;130;;;;;;https://x.test;0;\n"), false);
        assertEquals(0, r.ajoutes());
        assertEquals(3, r.erreurs().size());
        assertTrue(r.erreurs().get(0).startsWith("Ligne 2") && r.erreurs().get(0).contains("source"));
        verify(repo, never()).save(any());
    }

    @Test
    void importIgnoreLesMoteursDejaPresentsSaufSiEcraser() {
        MoteurReferenceRepository repo = mock(MoteurReferenceRepository.class);
        MoteurReference existant = gti();
        existant.setId(5L);
        when(repo.findFirstByMarqueIgnoreCaseAndModeleIgnoreCaseAndMotorisationIgnoreCaseAndAnneeDebut("Volkswagen", "Golf 7 GTI", "2.0 TSI", 2012))
                .thenReturn(Optional.of(existant));
        when(repo.findById(5L)).thenReturn(Optional.of(existant));
        MoteurReferenceServiceImpl s = new MoteurReferenceServiceImpl(repo);
        String ligne = ENTETE + "Volkswagen;Golf 7 GTI;2.0 TSI;Essence;2012;2019;220;350;2500;5500;6500;310;430;;;;;https://x.test;1;\n";
        var ignore = s.importerCsv(csv(ligne), false);
        assertEquals(1, ignore.ignores());
        verify(repo, never()).save(any());
        var maj = s.importerCsv(csv(ligne), true);
        assertEquals(1, maj.misAJour());
        verify(repo, times(1)).save(any());
        assertEquals(310, existant.getStage1Ch());
        assertTrue(existant.isVerifie());
    }

    @Test
    void importRefuseUnEnTeteIncompletEtLesFichiersBinaires() {
        MoteurReferenceServiceImpl s = new MoteurReferenceServiceImpl(mock(MoteurReferenceRepository.class));
        var e = assertThrows(IllegalArgumentException.class, () -> s.importerCsv(csv("marque;modele\nA;B\n"), false));
        assertTrue(e.getMessage().contains("Colonnes manquantes"));
        assertThrows(IllegalArgumentException.class, () -> s.importerCsv(new ByteArrayInputStream(new byte[]{'a', 0, 'b'}), false));
    }

    @Test
    void exportNeutraliseLesFormulesEtSeRelitSansPerte() {
        MoteurReferenceRepository repo = mock(MoteurReferenceRepository.class);
        MoteurReference m = gti();
        m.setSource("=HYPERLINK(\"http://x\")");
        when(repo.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(m));
        MoteurReferenceServiceImpl s = new MoteurReferenceServiceImpl(repo);
        String out = s.exporterCsv();
        assertTrue(out.startsWith("marque;modele;"));
        assertTrue(out.contains("'=HYPERLINK"), "une cellule commençant par = est préfixée pour ne pas être exécutée");
        assertTrue(out.contains("Golf 7 GTI") && out.contains(";300;420;"));
    }
}
