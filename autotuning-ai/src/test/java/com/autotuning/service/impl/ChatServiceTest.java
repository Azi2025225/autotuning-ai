package com.autotuning.service.impl;

import com.autotuning.model.Categorie;
import com.autotuning.model.Commande;
import com.autotuning.model.LigneCommande;
import com.autotuning.model.MoteurReference;
import com.autotuning.model.Produit;
import com.autotuning.model.StatutCommande;
import com.autotuning.service.*;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private final Categorie jantes = new Categorie("Jantes", "Jantes sport en alliage");

    private Produit produit(long id, String nom, String prix, int stock) {
        Produit p = new Produit();
        p.setId(id); p.setNom(nom); p.setPrix(new BigDecimal(prix)); p.setStock(stock); p.setCategorie(jantes);
        p.setCompatibilite("Universel");
        return p;
    }

    private MoteurReference golf() {
        MoteurReference m = new MoteurReference();
        m.setMarque("Volkswagen"); m.setModele("Golf 7"); m.setMotorisation("2.0 TDI CR 150 ch"); m.setCarburant("Diesel");
        m.setAnneeDebut(2012); m.setAnneeFin(2016); m.setPuissanceOrigine(150); m.setCoupleOrigine(320);
        m.setRegimeCouple(1750); m.setRegimePuissance(3750); m.setRegimeMax(4500);
        m.setStage1Ch(190); m.setStage1Nm(420);
        m.setSource("https://exemple.test/golf"); m.setVerifie(false);
        return m;
    }

    private Commande commande() {
        Commande c = new Commande();
        c.setId(7L); c.setStatut(StatutCommande.EXPEDIEE); c.setTotal(new BigDecimal("450.00"));
        LigneCommande l = new LigneCommande();
        l.setProduit(produit(1, "Jantes sportives 19\"", "450", 20)); l.setQuantite(1); l.setPrixUnitaire(new BigDecimal("450"));
        c.ajouterLigne(l);
        return c;
    }

    private ChatServiceImpl service(List<Produit> produitsEnBase, List<MoteurReference> moteursEnBase, List<Commande> commandesDeA) {
        ConseillerIAService ia = mock(ConseillerIAService.class);
        when(ia.iaReelleConfiguree()).thenReturn(false);
        ProduitService produits = mock(ProduitService.class);
        when(produits.tous()).thenReturn(produitsEnBase);
        when(produits.rechercher(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        CommandeService commandes = mock(CommandeService.class);
        when(commandes.historique("a@b.c")).thenReturn(commandesDeA);
        MoteurReferenceService moteurs = mock(MoteurReferenceService.class);
        when(moteurs.tous()).thenReturn(moteursEnBase);
        when(moteurs.tableau(any())).thenReturn(List.of(
                new MoteurReferenceService.LigneStage("Origine", 150, 320, null, null, null),
                new MoteurReferenceService.LigneStage("Stage 1", 190, 420, 40, 100, 27)));
        CategorieService categories = mock(CategorieService.class);
        when(categories.toutes()).thenReturn(List.of(jantes));
        return new ChatServiceImpl(ia, produits, commandes, moteurs, categories);
    }

    private ChatServiceImpl service() { return service(List.of(), List.of(), List.of()); }

    @Test
    void sansIaUneReponseLocaleEstRenvoyee() {
        ChatService.Reponse r = service().repondre("a@b.c", "C'est quoi un Stage 1 ?", List.of());
        assertTrue(r.local());
        assertTrue(r.texte().contains("Stage 1"));
    }

    @Test
    void questionGarantieRappelleLesRisques() {
        assertTrue(service().repondre("a@b.c", "Est-ce que ça annule la garantie ?", null).texte().toLowerCase().contains("garantie"));
    }

    @Test
    void messageVideOuTropLongRefuse() {
        ChatServiceImpl s = service();
        assertThrows(IllegalArgumentException.class, () -> s.repondre("a@b.c", "  ", List.of()));
        assertThrows(IllegalArgumentException.class, () -> s.repondre("a@b.c", "x".repeat(501), List.of()));
    }

    @Test
    void limiteDeMessagesParFenetre() {
        ChatServiceImpl s = service();
        for (int i = 0; i < ChatServiceImpl.MAX_PAR_FENETRE; i++) s.repondre("spam@b.c", "bonjour", List.of());
        assertThrows(IllegalStateException.class, () -> s.repondre("spam@b.c", "bonjour", List.of()));
        assertDoesNotThrow(() -> s.repondre("autre@b.c", "bonjour", List.of()));
    }

    @Test
    void lePromptNeContientQueLeDernierHistoriqueEtLesRegles() {
        List<ChatService.Tour> h = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) h.add(new ChatService.Tour("user", "question" + i));
        String p = service().construirePrompt("a@b.c", "hello", h);
        assertFalse(p.contains("question0"));
        assertTrue(p.contains("question9"));
        assertTrue(p.contains("n'invente aucun chiffre"));
    }

    @Test
    void lePromptRefleteLeCatalogueActuelEtSignaleLesNouveautes() {
        ChatServiceImpl s = service(List.of(produit(1, "Ancien produit", "100", 10), produit(2, "Produit tout neuf", "250.50", 0)), List.of(), List.of());
        String p = s.construirePrompt("a@b.c", "Quoi de neuf ?", List.of());
        assertTrue(p.contains("Produit tout neuf"));
        assertTrue(p.contains("250.5 DT"));
        assertTrue(p.contains("rupture de stock"));
        assertTrue(p.contains("NOUVEAUTÉ"));
    }

    @Test
    void lePromptNeContientQueLesCommandesDuClientConnecte() {
        ChatServiceImpl s = service(List.of(), List.of(), List.of(commande()));
        String pourA = s.construirePrompt("a@b.c", "Où en est ma commande ?", List.of());
        assertTrue(pourA.contains("n°7") && pourA.contains("Expédiée"));
        String pourAutre = s.construirePrompt("autre@b.c", "Où en est ma commande ?", List.of());
        assertFalse(pourAutre.contains("n°7"), "les commandes d'un autre client ne doivent jamais apparaître");
        assertTrue(pourAutre.contains("(aucune commande)"));
    }

    @Test
    void lePromptDonneLesChiffresDuMoteurCiteAvecSonStatut() {
        ChatServiceImpl s = service(List.of(), List.of(golf()), List.of());
        String p = s.construirePrompt("a@b.c", "Gain Stage 1 sur ma Golf 7 150 ch ?", List.of());
        assertTrue(p.contains("190 ch / 420 Nm"));
        assertTrue(p.contains("non mesurées"));
        assertTrue(p.contains("https://exemple.test/golf"));
    }

    @Test
    void reponseLocaleMoteurUtiliseLaBaseAvecSource() {
        ChatServiceImpl s = service(List.of(), List.of(golf()), List.of());
        String r = s.repondre("a@b.c", "Quel gain stage 1 sur Golf 7 2.0 TDI 150 ch ?", List.of()).texte();
        assertTrue(r.contains("190 ch / 420 Nm"));
        assertTrue(r.contains("Source : https://exemple.test/golf"));
        assertTrue(r.contains("non mesurées"));
    }

    @Test
    void moteurAbsentDeLaBaseNInventeAucunChiffre() {
        ChatServiceImpl s = service(List.of(), List.of(golf()), List.of());
        String r = s.repondre("a@b.c", "Quel gain en stage 1 sur une Tesla Model 3 ?", List.of()).texte();
        assertFalse(r.contains("190"));
        assertFalse(r.contains("420"));
    }

    @Test
    void reponseLocaleCommandeMontreLeVraiStatut() {
        ChatServiceImpl s = service(List.of(), List.of(), List.of(commande()));
        String r = s.repondre("a@b.c", "Où en est ma commande ?", List.of()).texte();
        assertTrue(r.contains("n°7"));
        assertTrue(r.contains("Expédiée"));
        assertTrue(s.repondre("autre@b.c", "Où en est ma commande ?", List.of()).texte().contains("pas encore passé de commande"));
    }

    @Test
    void reponseLocaleNouveauxProduitsListeLesPlusRecents() {
        ChatServiceImpl s = service(List.of(produit(1, "Ancien", "100", 10), produit(2, "Tout neuf", "200", 3)), List.of(), List.of());
        String r = s.repondre("a@b.c", "Quels sont vos nouveaux produits ?", List.of()).texte();
        assertTrue(r.indexOf("Tout neuf") >= 0 && r.indexOf("Tout neuf") < r.indexOf("Ancien"));
        assertTrue(r.contains("plus que 3 en stock"));
    }
}
