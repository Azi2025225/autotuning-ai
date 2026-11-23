package com.autotuning.service.impl;

import com.autotuning.service.*;

import com.autotuning.config.IAProperties;
import com.autotuning.dto.ReponseIA;
import com.autotuning.exception.IAIndisponibleException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConseillerIAServiceTest {

    private ConseillerIAServiceImpl service(String provider, String cle) {
        return new ConseillerIAServiceImpl(new IAProperties(provider, cle, null, 5), new ObjectMapper(), null, null, null,
                new OllamaClientImpl(new com.autotuning.config.OllamaProperties("http://localhost:1", null, "m", null, 2)), null);
    }

    @Test
    void modeDemoRenvoieTroisStagesEtUnAvertissement() {
        ConseillerIAServiceImpl s = service("mock", null);
        ReponseIA r = s.verifier(s.interroger("x"));
        assertEquals(3, r.stages().size());
        assertNull(r.estimation(), "aucune estimation chiffrée ne doit être inventée");
        assertFalse(r.avertissement().isBlank());
    }

    @Test
    void uneEstimationChiffreeVenantDeLIAEstIgnoree() {
        ConseillerIAServiceImpl s = service("mock", null);
        String json = s.interroger("x").replace("\"estimation\":null", "\"estimation\":\"+80 ch garantis\"");
        assertNull(s.verifier(json).estimation());
    }

    @Test
    void reponseSansTroisStagesEstRefusee() {
        assertThrows(IAIndisponibleException.class,
                () -> service("mock", null).verifier("{\"stages\":[],\"avertissement\":\"a\"}"));
    }

    @Test
    void reponseNonJsonEstRefusee() {
        assertThrows(IAIndisponibleException.class, () -> service("mock", null).verifier("pas du json"));
    }

    @Test
    void sansCleApiLeServiceSignaleUneIndisponibiliteControlee() {
        assertThrows(IAIndisponibleException.class, () -> service("gemini", "").interroger("x"));
        assertThrows(IAIndisponibleException.class, () -> service("openai", null).interroger("x"));
    }

    @Test
    void ollamaInjoignableDonneUneErreurControlee() {
        assertThrows(IAIndisponibleException.class, () -> service("ollama", null).interroger("x"));
    }

    @Test
    void jsonEntoureDeTexteEstTolere() {
        ConseillerIAServiceImpl s = service("mock", null);
        assertEquals(3, s.verifier("Voici la réponse :\n" + s.interroger("x") + "\nBonne route !").stages().size());
    }

    @Test
    void avertissementParDefautSiAbsent() {
        ConseillerIAServiceImpl s = service("mock", null);
        String json = s.interroger("x").replaceAll("\"avertissement\":\"[^\"]*\"", "\"avertissement\":\"\"");
        assertTrue(s.verifier(json).avertissement().contains("professionnel"));
    }

    @Test
    void lePromptIntegreLesChiffresDeReferenceQuandLeMoteurEstConnu() {
        com.autotuning.model.MoteurReference ref = new com.autotuning.model.MoteurReference();
        ref.setMarque("Volkswagen"); ref.setModele("Golf 7"); ref.setMotorisation("2.0 TDI"); ref.setCarburant("Diesel");
        ref.setAnneeDebut(2012); ref.setAnneeFin(2016); ref.setPuissanceOrigine(150); ref.setCoupleOrigine(320); ref.setVerifie(false);
        MoteurReferenceService moteurs = org.mockito.Mockito.mock(MoteurReferenceService.class);
        org.mockito.Mockito.when(moteurs.trouver("Volkswagen", "Golf 7", 2014, "Diesel", 150)).thenReturn(java.util.Optional.of(ref));
        org.mockito.Mockito.when(moteurs.tableau(ref)).thenReturn(java.util.List.of(
                new MoteurReferenceService.LigneStage("Origine", 150, 320, null, null, null),
                new MoteurReferenceService.LigneStage("Stage 1", 190, 420, 40, 100, 27)));
        ConseillerIAServiceImpl s = new ConseillerIAServiceImpl(new IAProperties("mock", null, null, 5), new ObjectMapper(), null, null, null,
                new OllamaClientImpl(new com.autotuning.config.OllamaProperties("http://localhost:1", null, "m", null, 2)), moteurs);
        com.autotuning.dto.MoteurForm f = new com.autotuning.dto.MoteurForm();
        f.setMarque("Volkswagen"); f.setModele("Golf 7"); f.setAnnee(2014); f.setCarburant("Diesel"); f.setMotorisation("2.0 TDI"); f.setPuissanceCh(150);
        String p = s.construirePrompt(f);
        assertTrue(p.contains("DONNÉES DE RÉFÉRENCE"));
        assertTrue(p.contains("Stage 1 190 ch / 420 Nm"));
        assertTrue(p.contains("non mesurées"));
        f.setModele("Polo");
        assertFalse(s.construirePrompt(f).contains("DONNÉES DE RÉFÉRENCE ci-dessous") && s.construirePrompt(f).contains("Stage 1 190"));
        assertTrue(s.construirePrompt(f).contains("aucune donnée de référence"));
    }
}
