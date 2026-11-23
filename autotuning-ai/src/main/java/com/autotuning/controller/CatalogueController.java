package com.autotuning.controller;

import com.autotuning.service.CategorieService;
import com.autotuning.service.ProduitService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class CatalogueController {
    private final ProduitService produits;
    private final CategorieService categories;

    public CatalogueController(ProduitService produits, CategorieService categories) {
        this.produits = produits; this.categories = categories;
    }

    @GetMapping("/catalogue")
    public String catalogue(@RequestParam(required = false) String q,
                            @RequestParam(required = false) Long categorie,
                            @RequestParam(required = false) String marque,
                            @RequestParam(required = false) BigDecimal prixMin,
                            @RequestParam(required = false) BigDecimal prixMax,
                            @RequestParam(required = false) String tri, Model m) {
        m.addAttribute("produits", produits.rechercher(q, categorie, marque, prixMin, prixMax, tri));
        m.addAttribute("categories", categories.toutes());
        m.addAttribute("q", q);
        m.addAttribute("categorieId", categorie);
        m.addAttribute("marque", marque);
        m.addAttribute("prixMin", prixMin);
        m.addAttribute("prixMax", prixMax);
        m.addAttribute("tri", tri);
        return "produits/catalogue";
    }

    @GetMapping("/produit/{id}")
    public String detail(@PathVariable Long id, Model m) {
        m.addAttribute("produit", produits.parId(id));
        return "produits/detail";
    }
}
