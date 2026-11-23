package com.autotuning.controller;

import com.autotuning.dto.MoteurForm;
import com.autotuning.exception.FichierInvalideException;
import com.autotuning.exception.IAIndisponibleException;
import com.autotuning.model.ConsultationIA;
import com.autotuning.model.Personnalisation;
import com.autotuning.service.*;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class IAController {
    private final ConseillerIAService conseiller;
    private final PersonnalisationService perso;
    private final ProduitService produits;
    private final ModeleVoitureService modeles;
    private final EditionImageService edition;
    private final MoteurReferenceService moteursRef;

    public IAController(ConseillerIAService conseiller, PersonnalisationService perso, ProduitService produits,
                        ModeleVoitureService modeles, EditionImageService edition, MoteurReferenceService moteursRef) {
        this.conseiller = conseiller; this.perso = perso; this.produits = produits;
        this.modeles = modeles; this.edition = edition; this.moteursRef = moteursRef;
    }

    /** Ajoute le tableau et les courbes si (et seulement si) un moteur de référence correspond au véhicule. */
    private void ajouterReference(Model m, com.autotuning.model.Vehicule v) {
        moteursRef.trouver(v.getMarque(), v.getModele(), v.getAnnee(), v.getCarburant(), v.getPuissanceCh()).ifPresent(ref -> {
            m.addAttribute("reference", ref);
            m.addAttribute("tableau", moteursRef.tableau(ref));
            m.addAttribute("courbes", moteursRef.courbes(ref));
        });
    }

    // ---------------------------------------------------------- Conseiller moteur
    @GetMapping("/ia/moteur")
    public String formulaire(Authentication auth, Model m) {
        m.addAttribute("form", new MoteurForm());
        m.addAttribute("historique", conseiller.historique(auth.getName()));
        return "ia/moteur";
    }

    @PostMapping("/ia/moteur")
    public String analyser(Authentication auth, @Valid @ModelAttribute("form") MoteurForm form, BindingResult br, Model m) {
        if (!br.hasErrors()) {
            try {
                ConsultationIA c = conseiller.analyser(auth.getName(), form);
                m.addAttribute("consultation", c);
                m.addAttribute("reponse", conseiller.lire(c));
                ajouterReference(m, c.getVehicule());
            } catch (IAIndisponibleException e) {
                m.addAttribute("erreurIA", e.getMessage());
            }
        }
        m.addAttribute("historique", conseiller.historique(auth.getName()));
        return "ia/moteur";
    }

    @GetMapping("/ia/moteur/{id}")
    public String consulter(Authentication auth, @PathVariable Long id, Model m) {
        ConsultationIA c = conseiller.consultation(auth.getName(), id);
        m.addAttribute("form", new MoteurForm());
        m.addAttribute("consultation", c);
        m.addAttribute("reponse", conseiller.lire(c));
        ajouterReference(m, c.getVehicule());
        m.addAttribute("historique", conseiller.historique(auth.getName()));
        return "ia/moteur";
    }

    // ---------------------------------------------------------- Personnalisation visuelle
    private void preparerPage(Authentication auth, Model m) {
        m.addAttribute("couleurs", PersonnalisationService.MODIFICATIONS);
        m.addAttribute("produits", produits.tous());
        m.addAttribute("modeles", modeles.tous());
        m.addAttribute("historique", perso.historique(auth.getName()));
        m.addAttribute("iaImage", edition.disponible());
    }

    @GetMapping("/ia/personnalisation")
    public String personnalisation(Authentication auth, @RequestParam(required = false) Long produit,
                                   @RequestParam(required = false) Long modele, Model m) {
        preparerPage(auth, m);
        m.addAttribute("produitChoisi", produit);
        m.addAttribute("modeleChoisi", modele);
        return "ia/personnalisation";
    }

    @PostMapping("/ia/personnalisation")
    public String generer(Authentication auth, @RequestParam(value = "photo", required = false) MultipartFile photo,
                          @RequestParam(required = false) Long modeleId, @RequestParam(required = false) Long produitId,
                          @RequestParam(required = false) String couleur, RedirectAttributes ra) {
        try {
            Personnalisation p = perso.creer(auth.getName(), photo, modeleId, produitId, couleur);
            return "redirect:/ia/personnalisation/" + p.getId();
        } catch (FichierInvalideException | IAIndisponibleException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/ia/personnalisation";
        }
    }

    @GetMapping("/ia/personnalisation/{id}")
    public String resultat(Authentication auth, @PathVariable Long id, Model m) {
        m.addAttribute("resultat", perso.parId(auth.getName(), id));
        preparerPage(auth, m);
        return "ia/personnalisation";
    }

    @GetMapping("/ia/personnalisation/{id}/image/{type}")
    public ResponseEntity<Resource> image(Authentication auth, @PathVariable Long id, @PathVariable String type,
                                          @RequestParam(defaultValue = "false") boolean telecharger) {
        Personnalisation p = perso.parId(auth.getName(), id);
        boolean apres = "apres".equals(type);
        Resource r = perso.image(p, apres);
        ResponseEntity.BodyBuilder b = ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .header("X-Content-Type-Options", "nosniff");
        if (telecharger) {
            b.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                    .filename("autotuning-simulation-" + id + ".png").build().toString());
        }
        return b.body(r);
    }
}
