package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.exception.FichierInvalideException;
import com.autotuning.exception.IAIndisponibleException;
import com.autotuning.exception.RessourceIntrouvableException;
import com.autotuning.model.ModeleVoiture;
import com.autotuning.model.Personnalisation;
import com.autotuning.model.Produit;
import com.autotuning.model.Utilisateur;
import com.autotuning.repository.PersonnalisationRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Essai visuel d'un kit du catalogue sur une voiture (photo du client ou modèle du catalogue).
 * Avec une clé Gemini, l'image est réellement éditée par l'IA ; sans clé, une simulation graphique
 * prédéfinie est affichée. Dans tous les cas, le résultat est une SIMULATION.
 */
@Service
public class PersonnalisationServiceImpl implements PersonnalisationService {

    private final PersonnalisationRepository repo;
    private final UtilisateurService utilisateurs;
    private final StockageImageService stockage;
    private final EditionImageService edition;
    private final ProduitService produits;
    private final ModeleVoitureService modeles;
    private final VerificationPhotoService verification;
    private final Path dossier;

    public PersonnalisationServiceImpl(PersonnalisationRepository repo, UtilisateurService utilisateurs,
                                   StockageImageService stockage, EditionImageService edition,
                                   ProduitService produits, ModeleVoitureService modeles,
                                   VerificationPhotoService verification) throws IOException {
        this.verification = verification;
        this.repo = repo;
        this.utilisateurs = utilisateurs;
        this.stockage = stockage;
        this.edition = edition;
        this.produits = produits;
        this.modeles = modeles;
        this.dossier = stockage.getRacine().resolve("prive");
        Files.createDirectories(dossier);
    }

    /**
     * @param fichier   photo du client (facultative si un modèle du catalogue est choisi)
     * @param modeleId  modèle de voiture du catalogue (facultatif si une photo est fournie)
     * @param produitId kit/pièce du catalogue à appliquer (facultatif si une couleur est choisie)
     * @param couleur   changement de couleur prédéfini (si aucun produit n'est choisi)
     */
    @Override
    public Personnalisation creer(String email, MultipartFile fichier, Long modeleId, Long produitId, String couleur) {
        // ----- source
        BufferedImage original;
        String nomVoiture = null;
        String marque = null;
        String descriptionVoiture = null;
        if (fichier != null && !fichier.isEmpty()) {
            original = stockage.valider(fichier);
            Optional<VerificationPhotoService.Resultat> v = verification.analyser(original);
            if (v.isPresent()) {
                if (!v.get().voiture()) {
                    throw new FichierInvalideException("Cette photo ne semble pas montrer une voiture. Importez une photo de véhicule bien visible.");
                }
                descriptionVoiture = v.get().description();
            }
        } else if (modeleId != null) {
            ModeleVoiture m = modeles.parId(modeleId);
            original = stockage.lireImagePublique(m.getImageUrl())
                    .orElseThrow(() -> new FichierInvalideException("La photo de ce modèle est introuvable."));
            nomVoiture = m.getLibelle();
            descriptionVoiture = nomVoiture;
            marque = m.getMarque();
        } else {
            throw new FichierInvalideException("Choisissez un modèle du catalogue ou importez votre photo.");
        }

        // ----- modification
        BufferedImage resultat;
        String code, libelle, avertissement = null;
        boolean ia = false;
        Long idProduit = null;
        if (produitId != null) {
            Produit p = produits.parId(produitId);
            idProduit = p.getId();
            code = "KIT";
            libelle = p.getNom() + (nomVoiture != null ? " sur " + nomVoiture : "");
            if (marque != null && p.getCompatibilite() != null && !p.getCompatibilite().isBlank()
                    && !p.getCompatibilite().toLowerCase().contains(marque.toLowerCase())
                    && !p.getCompatibilite().toLowerCase().contains("universel")) {
                avertissement = "Ce produit n'est pas indiqué compatible avec la marque " + marque + " : vérifiez la compatibilité avant achat.";
            }
            BufferedImage edite = null;
            String note = null;
            if (edition.disponible()) {
                try {
                    edite = edition.appliquerKit(original, stockage.lirePublic(p.getImageUrl()), p.getNom(), p.getCategorie().getNom(), descriptionVoiture);
                } catch (IAIndisponibleException e) {
                    // L'IA est configurée mais a échoué : on ne laisse pas le client sans résultat, on le prévient clairement.
                    note = e.getMessage() + " Un rendu graphique simplifié est affiché à la place.";
                }
            } else {
                note = "Mode démonstration (aucune clé d'édition IA) : rendu graphique simplifié.";
            }
            if (edite != null) {
                resultat = edite;
                ia = true;
            } else {
                resultat = appliquer(original, presetPour(p));
                avertissement = avertissement == null ? note : avertissement + " " + note;
            }
        } else if (couleur != null && MODIFICATIONS.contains(couleur)) {
            code = couleur;
            libelle = PersonnalisationService.libelle(couleur) + (nomVoiture != null ? " – " + nomVoiture : "");
            BufferedImage repeint = null;
            if (edition.disponible()) {
                try {
                    repeint = edition.appliquerCouleur(original, nomCouleurIA(couleur), descriptionVoiture);
                } catch (IAIndisponibleException e) {
                    avertissement = e.getMessage() + " Un rendu graphique simplifié est affiché à la place.";
                }
            }
            if (repeint != null) {
                resultat = repeint;
                ia = true;
            } else {
                resultat = appliquer(original, couleur);
            }
        } else {
            throw new FichierInvalideException("Choisissez un kit du catalogue ou une couleur.");
        }

        // ----- enregistrement
        Utilisateur u = utilisateurs.parEmail(email);
        String base = UUID.randomUUID().toString();
        try {
            ImageIO.write(original, "png", dossier.resolve(base + "-avant.png").toFile());
            ImageIO.write(resultat, "png", dossier.resolve(base + "-apres.png").toFile());
        } catch (IOException e) {
            throw new FichierInvalideException("Erreur lors de l'enregistrement de l'image.");
        }
        Personnalisation pe = new Personnalisation();
        pe.setUtilisateur(u);
        pe.setImageOriginale(base + "-avant.png");
        pe.setImageResultat(base + "-apres.png");
        pe.setModification(code);
        pe.setLibelle(libelle.length() > 250 ? libelle.substring(0, 250) : libelle);
        pe.setProduitId(idProduit);
        pe.setIa(ia);
        pe.setAvertissement(avertissement);
        return repo.save(pe);
    }

    @Override
    public List<Personnalisation> historique(String email) {
        return repo.findByUtilisateurIdOrderByDateCreationDesc(utilisateurs.parEmail(email).getId());
    }

    @Override
    public Personnalisation parId(String email, Long id) {
        return repo.findByIdAndUtilisateurId(id, utilisateurs.parEmail(email).getId())
                .orElseThrow(() -> new RessourceIntrouvableException("Personnalisation introuvable"));
    }

    @Override
    public Resource image(Personnalisation p, boolean apres) {
        Path f = dossier.resolve(apres ? p.getImageResultat() : p.getImageOriginale()).normalize();
        if (!f.startsWith(dossier) || !Files.exists(f)) throw new RessourceIntrouvableException("Image introuvable");
        return new FileSystemResource(f);
    }

    // ------------------------------------------------------------------ simulation graphique (sans clé IA)

    private static String nomCouleurIA(String couleur) {
        return switch (couleur) {
            case "COULEUR_ROUGE" -> "red";
            case "COULEUR_BLEU" -> "blue";
            case "COULEUR_VERT" -> "green";
            default -> "red";
        };
    }

    private static String presetPour(Produit p) {
        String c = p.getCategorie().getNom().toLowerCase();
        if (c.contains("jante")) return "JANTES";
        if (c.contains("spoiler")) return "SPOILER";
        return "KIT_CARROSSERIE";
    }

    private static BufferedImage appliquer(BufferedImage src, String modif) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, null);
        int w = src.getWidth(), h = src.getHeight();
        switch (modif) {
            case "COULEUR_ROUGE" -> teinte(out, 0f);
            case "COULEUR_BLEU" -> teinte(out, 0.6f);
            case "COULEUR_VERT" -> teinte(out, 0.33f);
            case "JANTES" -> {
                for (double cx : new double[]{0.27, 0.74}) {
                    double r = w * 0.065, x = w * cx, y = h * 0.70;
                    g.setColor(new Color(20, 20, 20, 235));
                    g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
                    g.setColor(new Color(190, 190, 195));
                    g.setStroke(new BasicStroke(Math.max(2f, (float) r * 0.12f)));
                    for (int i = 0; i < 5; i++) {
                        double a = i * 2 * Math.PI / 5;
                        g.drawLine((int) x, (int) y, (int) (x + r * 0.9 * Math.cos(a)), (int) (y + r * 0.9 * Math.sin(a)));
                    }
                    g.draw(new Ellipse2D.Double(x - r * 0.95, y - r * 0.95, 1.9 * r, 1.9 * r));
                }
            }
            case "SPOILER" -> {
                g.setColor(new Color(15, 15, 15, 240));
                Path2D p = new Path2D.Double();
                p.moveTo(w * 0.60, h * 0.36); p.lineTo(w * 0.86, h * 0.34);
                p.lineTo(w * 0.87, h * 0.38); p.lineTo(w * 0.61, h * 0.41); p.closePath();
                g.fill(p);
            }
            case "KIT_CARROSSERIE" -> {
                g.setColor(new Color(15, 15, 15, 225));
                g.fillRoundRect((int) (w * 0.12), (int) (h * 0.78), (int) (w * 0.76), (int) (h * 0.05), 12, 12);
                g.setColor(new Color(215, 25, 32, 230));
                g.fillRect((int) (w * 0.14), (int) (h * 0.835), (int) (w * 0.72), Math.max(3, (int) (h * 0.008)));
            }
            default -> { }
        }
        g.dispose();
        return out;
    }

    private static void teinte(BufferedImage img, float hue) {
        float[] hsb = new float[3];
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                Color.RGBtoHSB((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, hsb);
                if (hsb[1] > 0.08f && hsb[2] > 0.12f) {
                    img.setRGB(x, y, Color.HSBtoRGB(hue, Math.min(1f, hsb[1] * 1.2f + 0.25f), hsb[2]));
                } else if (hsb[1] <= 0.08f && hsb[2] > 0.35f && hsb[2] < 0.95f) {
                    img.setRGB(x, y, Color.HSBtoRGB(hue, 0.55f, hsb[2]));
                }
            }
        }
    }
}
