package com.autotuning.config;

import com.autotuning.model.*;
import com.autotuning.repository.CategorieRepository;
import com.autotuning.service.MoteurReferenceService;
import com.autotuning.repository.ProduitRepository;
import com.autotuning.repository.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

/** Données de démonstration : compte admin, catégories et produits (uniquement si la base est vide). */
@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UtilisateurRepository users;
    private final CategorieRepository categories;
    private final ProduitRepository produits;
    private final PasswordEncoder encoder;
    private final MoteurReferenceService moteursRef;
    private final boolean seedMoteurs;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(UtilisateurRepository users, CategorieRepository categories, ProduitRepository produits,
                           PasswordEncoder encoder, MoteurReferenceService moteursRef,
                           @Value("${app.moteurs.seed:true}") boolean seedMoteurs,
                           @Value("${app.admin.email:admin@autotuning.tn}") String adminEmail,
                           @Value("${app.admin.password:Admin@12345}") String adminPassword) {
        this.users = users; this.categories = categories; this.produits = produits; this.encoder = encoder; this.moteursRef = moteursRef; this.seedMoteurs = seedMoteurs;
        this.adminEmail = adminEmail; this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (!users.existsByEmailIgnoreCase(adminEmail)) {
            Utilisateur a = new Utilisateur();
            a.setNom("Admin"); a.setPrenom("AutoTuning"); a.setEmail(adminEmail);
            a.setMotDePasse(encoder.encode(adminPassword)); a.setRole(Role.ADMIN);
            users.save(a);
            log.info("Compte administrateur créé : {}", adminEmail);
        }
        if (seedMoteurs) chargerMoteursReference();
        if (categories.count() > 0) return;

        Categorie kits = categories.save(new Categorie("Kits carrosserie", "Kits complets pour transformer l'allure du véhicule"));
        Categorie pareChocs = categories.save(new Categorie("Pare-chocs", "Pare-chocs avant et arrière sport"));
        Categorie spoilers = categories.save(new Categorie("Spoilers", "Spoilers et becquets"));
        Categorie jantes = categories.save(new Categorie("Jantes", "Jantes sport en alliage"));
        Categorie diffuseurs = categories.save(new Categorie("Diffuseurs", "Diffuseurs arrière"));
        Categorie interieur = categories.save(new Categorie("Accessoires intérieurs", "Volants, pommeaux, tapis et plus"));

        p("Pare-chocs avant Sport M", "Pare-chocs avant style M, compatible avec plusieurs modèles BMW. Fabriqué en ABS de haute qualité, design sportif, installation facile.", "299", 12, "BMW Série 3 (F30), Série 4 (F32), Série 5 (G30)", "BMW", pareChocs, "/img/pare-chocs.svg");
        p("Jantes sportives 19\"", "Jantes alliage 19 pouces, finition noir mat, design à branches multiples.", "450", 20, "Universel 5x112 / 5x120", "Universel", jantes, "/img/jantes.svg");
        p("Spoiler arrière carbone", "Spoiler en fibre de carbone, finition brillante, fixation par collage 3M.", "350", 8, "BMW Série 3, Audi A4, Mercedes Classe C", "Universel", spoilers, "/img/spoiler.svg");
        p("Diffuseur arrière", "Diffuseur arrière sport avec ailettes, ABS noir brillant.", "320", 10, "BMW Série 3 (F30), Série 4 (F32)", "BMW", diffuseurs, "/img/diffuseur.svg");
        p("Kit carrosserie complet", "Kit complet : pare-chocs avant/arrière, bas de caisse et diffuseur.", "1200", 3, "BMW Série 3 (F30)", "BMW", kits, "/img/kit.svg");
        p("Volant sport cuir perforé", "Volant sport méplat en cuir perforé avec surpiqûres rouges.", "260", 15, "Universel (adaptateur selon modèle)", "Universel", interieur, "/img/interieur.svg");
        p("Pare-chocs arrière Sport", "Pare-chocs arrière avec découpe pour diffuseur, ABS haute qualité.", "310", 6, "Audi A4 (B9), Mercedes Classe C (W205)", "Audi", pareChocs, "/img/pare-chocs.svg");
        p("Jantes forgées 20\" Argent", "Jantes forgées légères 20 pouces, finition argent usiné.", "980", 0, "Universel 5x112", "Universel", jantes, "/img/jantes.svg");
        log.info("Données de démonstration insérées");
    }

    private void p(String nom, String desc, String prix, int stock, String compat, String marque, Categorie cat, String img) {
        Produit p = new Produit();
        p.setNom(nom); p.setDescription(desc); p.setPrix(new BigDecimal(prix)); p.setStock(stock);
        p.setCompatibilite(compat); p.setMarque(marque); p.setCategorie(cat); p.setImageUrl(img);
        produits.save(p);
    }

    /**
     * Charge data/moteurs_reference.csv : uniquement des valeurs lues sur des pages publiques de préparateurs (valeurs COMMERCIALES
     * annoncées, non mesurées), toutes marquées « à confirmer » avec leur source. Seuls les moteurs absents sont ajoutés :
     * les modifications faites par l'administrateur ne sont jamais écrasées.
     */
    private void chargerMoteursReference() {
        try (InputStream in = new ClassPathResource("data/moteurs_reference.csv").getInputStream()) {
            MoteurReferenceService.ResumeImport r = moteursRef.importerCsv(in, false);
            if (r.ajoutes() > 0 || !r.erreurs().isEmpty()) {
                log.info("Moteurs de référence : {} ajouté(s), {} déjà présent(s), {} erreur(s)", r.ajoutes(), r.ignores(), r.erreurs().size());
                r.erreurs().forEach(e -> log.warn("Import moteurs : {}", e));
            }
        } catch (IOException | RuntimeException e) {
            log.warn("Moteurs de référence non chargés : {}", e.getMessage());
        }
    }
}
