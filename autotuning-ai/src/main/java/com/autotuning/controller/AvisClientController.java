package com.autotuning.controller;

import com.autotuning.dto.AvisClientForm;
import com.autotuning.service.AvisClientService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AvisClientController {
    private final AvisClientService service;

    public AvisClientController(AvisClientService service) {
        this.service = service;
    }

    @GetMapping("/avis")
    public String pageAvis(Authentication authentication, Model model) {
        model.addAttribute("avis", service.tous());
        model.addAttribute("moyenne", service.noteMoyenne());
        model.addAttribute("form", new AvisClientForm());
        model.addAttribute("aDejaDonneUnAvis",
                authentication != null && service.aDejaDonneUnAvis(authentication.getName()));
        return "avis/liste";
    }

    @PostMapping("/avis")
    public String publierAvis(Authentication authentication, @Valid @ModelAttribute("form") AvisClientForm form,
                              BindingResult resultat, Model model, RedirectAttributes redirectAttributes) {
        if (resultat.hasErrors()) {
            chargerPage(model, authentication, form);
            return "avis/liste";
        }
        try {
            service.ajouter(authentication.getName(), form.getNote(), form.getCommentaire());
            redirectAttributes.addFlashAttribute("succes", "Merci pour votre avis !");
            return "redirect:/avis";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/avis";
        }
    }

    private void chargerPage(Model model, Authentication authentication, AvisClientForm form) {
        model.addAttribute("avis", service.tous());
        model.addAttribute("moyenne", service.noteMoyenne());
        model.addAttribute("form", form);
        model.addAttribute("aDejaDonneUnAvis", service.aDejaDonneUnAvis(authentication.getName()));
    }
}
