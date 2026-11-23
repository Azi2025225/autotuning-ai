package com.autotuning.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RessourceIntrouvableException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String introuvable(RessourceIntrouvableException e, Model model) {
        model.addAttribute("titre", "Page introuvable");
        model.addAttribute("message", e.getMessage());
        return "error";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String pasTrouve(Model model) {
        model.addAttribute("titre", "Page introuvable");
        model.addAttribute("message", "La page demandée n'existe pas.");
        return "error";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tropGros(RedirectAttributes ra, HttpServletRequest req) {
        ra.addFlashAttribute("erreur", "Fichier trop volumineux (5 Mo maximum).");
        String cible = "/";
        String ref = req.getHeader("Referer");
        if (ref != null) {
            try {
                URI u = URI.create(ref);
                String c = u.getRawPath() + (u.getRawQuery() != null ? "?" + u.getRawQuery() : "");
                if (c.startsWith("/") && !c.startsWith("//")) cible = c;
            } catch (RuntimeException ignored) { }
        }
        return "redirect:" + cible;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String generique(Exception e, Model model) {
        log.error("Erreur inattendue", e);
        model.addAttribute("titre", "Une erreur est survenue");
        model.addAttribute("message", "Veuillez réessayer plus tard. L'incident a été journalisé.");
        return "error";
    }
}
