package com.autotuning.controller;

import com.autotuning.exception.StockInsuffisantException;
import com.autotuning.model.LignePanier;
import com.autotuning.model.Commande;
import com.autotuning.service.CommandeService;
import com.autotuning.service.PanierService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class PanierController {
    private final PanierService panier;
    private final CommandeService commandes;

    public PanierController(PanierService panier, CommandeService commandes) {
        this.panier = panier; this.commandes = commandes;
    }

    @GetMapping("/panier")
    public String voir(Authentication auth, Model m) {
        List<LignePanier> lignes = panier.lignes(auth.getName());
        m.addAttribute("lignes", lignes);
        m.addAttribute("total", panier.total(lignes));
        return "panier/panier";
    }

    @PostMapping("/panier/ajouter")
    public String ajouter(Authentication auth, @RequestParam Long produitId, @RequestParam(defaultValue = "1") int quantite,
                          @RequestHeader(value = "Referer", required = false) String referer, RedirectAttributes ra) {
        try {
            panier.ajouter(auth.getName(), produitId, quantite);
            ra.addFlashAttribute("succes", "Produit ajouté au panier.");
        } catch (StockInsuffisantException | IllegalArgumentException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/panier";
    }

    @PostMapping("/panier/modifier")
    public String modifier(Authentication auth, @RequestParam Long ligneId, @RequestParam int quantite, RedirectAttributes ra) {
        try {
            panier.modifier(auth.getName(), ligneId, quantite);
        } catch (StockInsuffisantException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/panier";
    }

    @PostMapping("/panier/retirer")
    public String retirer(Authentication auth, @RequestParam Long ligneId) {
        panier.retirer(auth.getName(), ligneId);
        return "redirect:/panier";
    }

    @PostMapping("/commandes")
    public String commander(Authentication auth, RedirectAttributes ra) {
        try {
            Commande c = commandes.creerCommande(auth.getName());
            ra.addFlashAttribute("succes", "Commande n°" + c.getId() + " confirmée (paiement simulé). Merci !");
            return "redirect:/commandes/" + c.getId();
        } catch (StockInsuffisantException | IllegalStateException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/panier";
        }
    }

    @GetMapping("/commandes")
    public String historique(Authentication auth, Model m) {
        m.addAttribute("commandes", commandes.historique(auth.getName()));
        return "commandes/liste";
    }

    @GetMapping("/commandes/{id}")
    public String detail(Authentication auth, @PathVariable Long id, Model m) {
        m.addAttribute("commande", commandes.detailClient(auth.getName(), id));
        return "commandes/detail";
    }
}
