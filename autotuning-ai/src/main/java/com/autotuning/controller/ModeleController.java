package com.autotuning.controller;

import com.autotuning.service.ModeleVoitureService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ModeleController {
    private final ModeleVoitureService modeles;

    public ModeleController(ModeleVoitureService modeles) { this.modeles = modeles; }

    @GetMapping("/modeles")
    public String liste(Model m) {
        m.addAttribute("modeles", modeles.tous());
        return "modeles/liste";
    }
}
