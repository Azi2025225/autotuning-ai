package com.autotuning.controller;

import com.autotuning.service.PanierService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Expose le nombre d'articles du panier à la barre de navigation. */
@ControllerAdvice
public class GlobalModelAdvice {
    private final PanierService panier;

    public GlobalModelAdvice(PanierService panier) { this.panier = panier; }

    @ModelAttribute("nbPanier")
    public int nbPanier(Authentication auth) {
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) return 0;
        try {
            return panier.nombreArticles(auth.getName());
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
