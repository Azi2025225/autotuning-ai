package com.autotuning.controller;

import com.autotuning.service.CategorieService;
import com.autotuning.service.ProduitService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final ProduitService produits;
    private final CategorieService categories;

    public HomeController(ProduitService produits, CategorieService categories) {
        this.produits = produits; this.categories = categories;
    }

    @GetMapping("/")
    public String accueil(Model m) {
        m.addAttribute("populaires", produits.populaires());
        m.addAttribute("categories", categories.toutes());
        return "index";
    }

    @GetMapping("/acces-refuse")
    public String refuse(Model m) {
        m.addAttribute("titre", "Accès refusé");
        m.addAttribute("message", "Vous n'avez pas les droits nécessaires pour accéder à cette page.");
        return "error";
    }
}
