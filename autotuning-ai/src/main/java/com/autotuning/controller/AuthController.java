package com.autotuning.controller;

import com.autotuning.dto.InscriptionForm;
import com.autotuning.dto.ProfilForm;
import com.autotuning.model.Utilisateur;
import com.autotuning.service.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Value;
import com.autotuning.dto.ReinitialisationMotDePasseForm;
import com.autotuning.service.ReinitialisationMotDePasseService;

@Controller
public class AuthController {
    private final UtilisateurService service;
    private final ReinitialisationMotDePasseService reinitialisation;
    private final boolean googleDisponible;

    public AuthController(UtilisateurService service, ReinitialisationMotDePasseService reinitialisation,
                          @Value("${app.google.client-id:}") String googleClientId,
                          @Value("${app.google.client-secret:}") String googleClientSecret) {
        this.service = service;
        this.reinitialisation = reinitialisation;
        this.googleDisponible = !googleClientId.isBlank() && !googleClientSecret.isBlank();
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("googleDisponible", googleDisponible);
        return "auth/login";
    }

    @GetMapping("/mot-de-passe-oublie")
    public String motDePasseOublie() { return "auth/mot-de-passe-oublie"; }

    @PostMapping("/mot-de-passe-oublie")
    public String demanderReinitialisation(@RequestParam String email, RedirectAttributes ra) {
        reinitialisation.demander(email);
        ra.addFlashAttribute("message", "Si cette adresse correspond à un compte, un lien de réinitialisation sera envoyé.");
        return "redirect:/mot-de-passe-oublie";
    }

    @GetMapping("/reinitialiser-mot-de-passe")
    public String formulaireReinitialisation(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("form", new ReinitialisationMotDePasseForm());
        model.addAttribute("token", token);
        model.addAttribute("tokenValide", reinitialisation.tokenValide(token));
        return "auth/reinitialiser-mot-de-passe";
    }

    @PostMapping("/reinitialiser-mot-de-passe")
    public String reinitialiser(@RequestParam String token,
            @Valid @ModelAttribute("form") ReinitialisationMotDePasseForm form, BindingResult br, Model model) {
        if (form.getMotDePasse() != null && !form.getMotDePasse().equals(form.getConfirmation())) {
            br.rejectValue("confirmation", "mismatch", "Les mots de passe ne correspondent pas.");
        }
        if (!reinitialisation.tokenValide(token)) {
            model.addAttribute("tokenValide", false);
            model.addAttribute("token", token);
            return "auth/reinitialiser-mot-de-passe";
        }
        if (!br.hasErrors()) {
            if (reinitialisation.reinitialiser(token, form.getMotDePasse())) {
                return "redirect:/login?motDePasseReinitialise";
            }
            model.addAttribute("tokenValide", false);
        } else {
            model.addAttribute("tokenValide", true);
        }
        model.addAttribute("token", token);
        return "auth/reinitialiser-mot-de-passe";
    }

    @GetMapping("/inscription")
    public String inscription(Model m) {
        m.addAttribute("form", new InscriptionForm());
        return "auth/inscription";
    }

    @PostMapping("/inscription")
    public String inscrire(@Valid @ModelAttribute("form") InscriptionForm form, BindingResult br, RedirectAttributes ra) {
        if (form.getMotDePasse() != null && !form.getMotDePasse().equals(form.getConfirmation())) {
            br.rejectValue("confirmation", "mismatch", "Les mots de passe ne correspondent pas");
        }
        if (!br.hasErrors()) {
            try {
                service.inscrire(form);
            } catch (IllegalArgumentException e) {
                br.rejectValue("email", "duplicate", e.getMessage());
            }
        }
        if (br.hasErrors()) return "auth/inscription";
        ra.addFlashAttribute("succes", "Compte créé ! Vous pouvez maintenant vous connecter.");
        return "redirect:/login";
    }

    @GetMapping("/profil")
    public String profil(Authentication auth, Model m) {
        Utilisateur u = service.parEmail(auth.getName());
        ProfilForm f = new ProfilForm();
        f.setNom(u.getNom()); f.setPrenom(u.getPrenom()); f.setTelephone(u.getTelephone());
        m.addAttribute("form", f);
        m.addAttribute("utilisateur", u);
        return "auth/profil";
    }

    @PostMapping("/profil")
    public String majProfil(Authentication auth, @Valid @ModelAttribute("form") ProfilForm form, BindingResult br,
                            Model m, RedirectAttributes ra) {
        if (!br.hasErrors()) {
            try {
                service.mettreAJourProfil(auth.getName(), form);
                ra.addFlashAttribute("succes", "Profil mis à jour.");
                return "redirect:/profil";
            } catch (IllegalArgumentException e) {
                br.rejectValue("nouveauMotDePasse", "invalid", e.getMessage());
            }
        }
        m.addAttribute("utilisateur", service.parEmail(auth.getName()));
        return "auth/profil";
    }
}
