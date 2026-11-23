package com.autotuning;

import com.autotuning.dto.InscriptionForm;
import com.autotuning.exception.StockInsuffisantException;
import com.autotuning.model.Commande;
import com.autotuning.model.Produit;
import com.autotuning.model.StatutCommande;
import com.autotuning.repository.ProduitRepository;
import com.autotuning.service.CommandeService;
import com.autotuning.service.PanierService;
import com.autotuning.service.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class CommandeServiceTest {
    @Autowired UtilisateurService utilisateurs;
    @Autowired PanierService panier;
    @Autowired CommandeService commandes;
    @Autowired ProduitRepository produits;

    private static final String EMAIL = "test.commande@example.com";
    private Produit produit;

    @BeforeEach
    void preparer() {
        InscriptionForm f = new InscriptionForm();
        f.setNom("Test"); f.setPrenom("Client"); f.setEmail(EMAIL); f.setMotDePasse("motdepasse1"); f.setConfirmation("motdepasse1");
        utilisateurs.inscrire(f);
        produit = produits.findAll().stream().filter(p -> p.getStock() >= 3).findFirst().orElseThrow();
    }

    @Test
    void emailDejaUtiliseEstRefuse() {
        InscriptionForm f = new InscriptionForm();
        f.setNom("X"); f.setPrenom("Y"); f.setEmail(EMAIL.toUpperCase()); f.setMotDePasse("motdepasse1");
        assertThrows(IllegalArgumentException.class, () -> utilisateurs.inscrire(f));
    }

    @Test
    void ajoutAuPanierMetAJourQuantiteEtTotal() {
        panier.ajouter(EMAIL, produit.getId(), 1);
        panier.ajouter(EMAIL, produit.getId(), 1);
        var lignes = panier.lignes(EMAIL);
        assertEquals(1, lignes.size());
        assertEquals(2, lignes.get(0).getQuantite());
        assertEquals(0, produit.getPrix().multiply(java.math.BigDecimal.valueOf(2)).compareTo(panier.total(lignes)));
    }

    @Test
    void stockInsuffisantRefuseLAjout() {
        assertThrows(StockInsuffisantException.class, () -> panier.ajouter(EMAIL, produit.getId(), produit.getStock() + 1));
    }

    @Test
    void confirmationEnregistreCommandeDecrementeStockEtViderLePanier() {
        int avant = produit.getStock();
        panier.ajouter(EMAIL, produit.getId(), 2);
        Commande c = commandes.creerCommande(EMAIL);
        assertNotNull(c.getId());
        assertEquals(1, c.getLignes().size());
        assertEquals(avant - 2, produits.findById(produit.getId()).orElseThrow().getStock());
        assertTrue(panier.lignes(EMAIL).isEmpty());
    }

    @Test
    void annulationRemetLesArticlesEnStock() {
        int avant = produit.getStock();
        panier.ajouter(EMAIL, produit.getId(), 2);
        Commande c = commandes.creerCommande(EMAIL);
        commandes.changerStatut(c.getId(), StatutCommande.ANNULEE);
        assertEquals(avant, produits.findById(produit.getId()).orElseThrow().getStock());
    }

    @Test
    void panierVideNePeutPasEtreCommande() {
        assertThrows(IllegalStateException.class, () -> commandes.creerCommande(EMAIL));
    }
}
