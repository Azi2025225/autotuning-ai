package com.autotuning.controller;

import com.autotuning.dto.MoteurRefForm;
import com.autotuning.dto.ProduitForm;
import com.autotuning.exception.FichierInvalideException;
import com.autotuning.model.Produit;
import com.autotuning.model.StatutCommande;
import com.autotuning.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.autotuning.model.Role;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final ProduitService produits;
    private final CategorieService categories;
    private final CommandeService commandes;
    private final UtilisateurService utilisateurs;
    private final StatistiquesService stats;
    private final ModeleVoitureService modeles;
    private final StockageImageService stockage;
    private final MoteurReferenceService moteursRef;

    public AdminController(ProduitService produits, CategorieService categories, CommandeService commandes,
                           UtilisateurService utilisateurs, StatistiquesService stats,
                           ModeleVoitureService modeles, StockageImageService stockage,
                           MoteurReferenceService moteursRef) {
        this.moteursRef = moteursRef;
        this.produits = produits; this.categories = categories; this.commandes = commandes;
        this.utilisateurs = utilisateurs; this.stats = stats; this.modeles = modeles; this.stockage = stockage;
    }

    @GetMapping
    public String dashboard(Model m) {
        m.addAttribute("nbProduits", stats.nbProduits());
        m.addAttribute("nbCommandes", stats.nbCommandes());
        m.addAttribute("nbClients", stats.nbClients());
        m.addAttribute("ca", stats.chiffreAffaires());
        m.addAttribute("recentes", commandes.recentes());
        m.addAttribute("ventes", stats.meilleuresVentes());
        m.addAttribute("nbConsultations", stats.nbConsultations());
        m.addAttribute("nbPersonnalisations", stats.nbPersonnalisations());
        m.addAttribute("stockFaible", stats.stockFaible().stream().limit(5).toList());
        m.addAttribute("section", "dashboard");
        return "admin/dashboard";
    }

    // ---- produits
    @GetMapping("/produits")
    public String listeProduits(@RequestParam(required = false) String q, @RequestParam(required = false) Long categorie, Model m) {
        m.addAttribute("produits", produits.rechercher(q, categorie, null, null, null, "nom"));
        m.addAttribute("categories", categories.toutes());
        m.addAttribute("q", q);
        m.addAttribute("categorieId", categorie);
        m.addAttribute("section", "produits");
        return "admin/produits";
    }

    @GetMapping("/produits/nouveau")
    public String nouveau(Model m) {
        m.addAttribute("form", new ProduitForm());
        m.addAttribute("categories", categories.toutes());
        m.addAttribute("section", "produits");
        return "admin/produit-form";
    }

    @GetMapping("/produits/{id}/modifier")
    public String modifier(@PathVariable Long id, Model m) {
        Produit p = produits.parId(id);
        ProduitForm f = new ProduitForm();
        f.setId(p.getId()); f.setNom(p.getNom()); f.setDescription(p.getDescription()); f.setPrix(p.getPrix());
        f.setStock(p.getStock()); f.setImageUrl(p.getImageUrl()); f.setCompatibilite(p.getCompatibilite());
        f.setMarque(p.getMarque()); f.setCategorieId(p.getCategorie().getId());
        m.addAttribute("form", f);
        m.addAttribute("categories", categories.toutes());
        m.addAttribute("section", "produits");
        return "admin/produit-form";
    }

    @PostMapping("/produits")
    public String enregistrer(@Valid @ModelAttribute("form") ProduitForm form, BindingResult br,
                              @RequestParam(value = "image", required = false) MultipartFile image,
                              Model m, RedirectAttributes ra) {
        if (!br.hasErrors() && image != null && !image.isEmpty()) {
            try {
                form.setImageUrl(stockage.sauverPublic(image, "produits"));
            } catch (FichierInvalideException e) {
                br.rejectValue("imageUrl", "image", e.getMessage());
            }
        }
        if (br.hasErrors()) {
            m.addAttribute("categories", categories.toutes());
            m.addAttribute("section", "produits");
            return "admin/produit-form";
        }
        produits.enregistrer(form);
        ra.addFlashAttribute("succes", "Produit enregistré.");
        return "redirect:/admin/produits";
    }

    @PostMapping("/produits/{id}/stock")
    public String majStock(@PathVariable Long id, @RequestParam int stock, RedirectAttributes ra) {
        if (stock < 0 || stock > 100000) {
            ra.addFlashAttribute("erreur", "Stock invalide.");
        } else {
            produits.majStock(id, stock);
            ra.addFlashAttribute("succes", "Stock mis à jour.");
        }
        return "redirect:/admin/produits";
    }

    @PostMapping("/produits/{id}/supprimer")
    public String supprimer(@PathVariable Long id, RedirectAttributes ra) {
        try {
            produits.supprimer(id);
            ra.addFlashAttribute("succes", "Produit supprimé.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/produits";
    }

    // ---- catégories
    @GetMapping("/categories")
    public String listeCategories(Model m) {
        m.addAttribute("categories", categories.toutes());
        m.addAttribute("section", "categories");
        return "admin/categories";
    }

    @PostMapping("/categories")
    public String enregistrerCategorie(@RequestParam(required = false) Long id, @RequestParam String nom,
                                       @RequestParam(required = false) String description, RedirectAttributes ra) {
        if (nom == null || nom.isBlank() || nom.length() > 100) {
            ra.addFlashAttribute("erreur", "Le nom de la catégorie est obligatoire (100 caractères maximum).");
        } else {
            try {
                categories.enregistrer(id, nom, description);
                ra.addFlashAttribute("succes", "Catégorie enregistrée.");
            } catch (IllegalArgumentException e) {
                ra.addFlashAttribute("erreur", e.getMessage());
            }
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/supprimer")
    public String supprimerCategorie(@PathVariable Long id, RedirectAttributes ra) {
        try {
            categories.supprimer(id);
            ra.addFlashAttribute("succes", "Catégorie supprimée.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ---- commandes
    @GetMapping("/commandes")
    public String listeCommandes(Model m) {
        m.addAttribute("commandes", commandes.toutes());
        m.addAttribute("statuts", StatutCommande.values());
        m.addAttribute("section", "commandes");
        return "admin/commandes";
    }

    @PostMapping("/commandes/{id}/statut")
    public String statut(@PathVariable Long id, @RequestParam StatutCommande statut, RedirectAttributes ra) {
        try {
            commandes.changerStatut(id, statut);
            ra.addFlashAttribute("succes", "Statut mis à jour.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/commandes";
    }

    // ---- clients
    @GetMapping("/clients")
    public String clients(Model m) {
        m.addAttribute("clients", utilisateurs.clients());
        m.addAttribute("section", "clients");
        return "admin/clients";
    }

    @PostMapping("/clients/{id}/acces")
    public String majAcces(@PathVariable Long id, @RequestParam Role role, @RequestParam boolean actif,
                           Authentication authentication, RedirectAttributes ra) {
        try {
            utilisateurs.mettreAJourAcces(id, role, actif, authentication.getName());
            ra.addFlashAttribute("succes", "Les accès de l'utilisateur ont été mis à jour.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/clients";
    }

    // ---- statistiques détaillées
    @GetMapping("/statistiques")
    public String statistiques(@RequestParam(defaultValue = "30") int jours, Model m) {
        int j = jours == 7 || jours == 90 ? jours : 30;
        m.addAllAttributes(stats.detail(j));
        m.addAttribute("section", "statistiques");
        return "admin/statistiques";
    }

    // ---- modèles de voitures
    @GetMapping("/modeles")
    public String listeModeles(Model m) {
        m.addAttribute("modeles", modeles.tous());
        m.addAttribute("section", "modeles");
        return "admin/modeles";
    }

    @PostMapping("/modeles")
    public String ajouterModele(@RequestParam String marque, @RequestParam String nom,
                                @RequestParam("image") MultipartFile image, RedirectAttributes ra) {
        if (marque.isBlank() || nom.isBlank() || marque.length() > 60 || nom.length() > 100) {
            ra.addFlashAttribute("erreur", "Marque et modèle sont obligatoires (60 / 100 caractères maximum).");
            return "redirect:/admin/modeles";
        }
        try {
            modeles.enregistrer(marque, nom, stockage.sauverPublic(image, "modeles"));
            ra.addFlashAttribute("succes", "Modèle ajouté au catalogue.");
        } catch (FichierInvalideException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/modeles";
    }

    @PostMapping("/modeles/{id}/supprimer")
    public String supprimerModele(@PathVariable Long id, RedirectAttributes ra) {
        modeles.supprimer(id);
        ra.addFlashAttribute("succes", "Modèle supprimé.");
        return "redirect:/admin/modeles";
    }

    // ---- moteurs de référence (données réelles Stage 1/2/3)
    @GetMapping("/moteurs")
    public String listeMoteurs(Model m) {
        m.addAttribute("moteurs", moteursRef.tous());
        m.addAttribute("section", "moteurs");
        return "admin/moteurs";
    }

    @GetMapping("/moteurs/nouveau")
    public String nouveauMoteur(Model m) {
        m.addAttribute("form", new MoteurRefForm());
        m.addAttribute("section", "moteurs");
        return "admin/moteur-form";
    }

    @GetMapping("/moteurs/{id}/modifier")
    public String modifierMoteur(@PathVariable Long id, Model m) {
        m.addAttribute("form", moteursRef.versForm(moteursRef.parId(id)));
        m.addAttribute("section", "moteurs");
        return "admin/moteur-form";
    }

    @PostMapping("/moteurs")
    public String enregistrerMoteur(@Valid @ModelAttribute("form") MoteurRefForm form, BindingResult br, Model m, RedirectAttributes ra) {
        if (!br.hasErrors()) {
            try {
                moteursRef.enregistrer(form);
                ra.addFlashAttribute("succes", "Moteur de référence enregistré.");
                return "redirect:/admin/moteurs";
            } catch (IllegalArgumentException e) {
                br.reject("moteur", e.getMessage());
            }
        }
        m.addAttribute("section", "moteurs");
        return "admin/moteur-form";
    }

    @PostMapping("/moteurs/{id}/supprimer")
    public String supprimerMoteur(@PathVariable Long id, RedirectAttributes ra) {
        moteursRef.supprimer(id);
        ra.addFlashAttribute("succes", "Moteur supprimé.");
        return "redirect:/admin/moteurs";
    }

    /** Import CSV : plusieurs moteurs d'un coup, chacun avec sa source (réservé à l'administrateur par /admin/**). */
    @PostMapping("/moteurs/import")
    public String importerMoteurs(@RequestParam("fichier") MultipartFile fichier,
                                  @RequestParam(defaultValue = "false") boolean ecraser, RedirectAttributes ra) {
        String nom = fichier.getOriginalFilename() == null ? "" : fichier.getOriginalFilename().toLowerCase();
        if (fichier.isEmpty() || !nom.endsWith(".csv")) {
            ra.addFlashAttribute("erreur", "Choisissez un fichier .csv.");
            return "redirect:/admin/moteurs";
        }
        try {
            MoteurReferenceService.ResumeImport r = moteursRef.importerCsv(fichier.getInputStream(), ecraser);
            String message = r.ajoutes() + " moteur(s) ajouté(s), " + r.misAJour() + " mis à jour, " + r.ignores() + " déjà présent(s) ignoré(s).";
            if (r.erreurs().isEmpty()) ra.addFlashAttribute("succes", message);
            else {
                ra.addFlashAttribute("erreur", message + " " + r.erreurs().size() + " ligne(s) refusée(s) :");
                ra.addFlashAttribute("erreursImport", r.erreurs().stream().limit(30).toList());
            }
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        } catch (IOException e) {
            ra.addFlashAttribute("erreur", "Lecture du fichier impossible.");
        }
        return "redirect:/admin/moteurs";
    }

    /** Export CSV de la base : sert aussi de modèle pour l'import (UTF-8 avec BOM, ouvrable dans Excel). */
    @GetMapping("/moteurs/export.csv")
    public ResponseEntity<byte[]> exporterMoteurs() {
        byte[] corps = ("\uFEFF" + moteursRef.exporterCsv()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("moteurs_reference.csv").build().toString())
                .body(corps);
    }
}
