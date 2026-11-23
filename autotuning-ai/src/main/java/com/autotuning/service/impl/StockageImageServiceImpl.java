package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.config.WebConfig;
import com.autotuning.exception.FichierInvalideException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Validation et stockage des images téléversées (produits, modèles de voitures, photos clients). */
@Service
public class StockageImageServiceImpl implements StockageImageService {
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png");
    private static final Set<String> DOSSIERS = Set.of("produits", "modeles");

    private final Path racine;
    private final Path media;

    public StockageImageServiceImpl(WebConfig cfg) throws IOException {
        this.racine = Path.of(cfg.getUploadDir()).toAbsolutePath().normalize();
        this.media = racine.resolve("media");
        Files.createDirectories(media);
    }

    @Override
    public Path getRacine() { return racine; }

    /** Contrôle type, taille, contenu réellement décodable et dimensions ; renvoie l'image normalisée (RVB, max 1600 px). */
    @Override
    public BufferedImage valider(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) throw new FichierInvalideException("Veuillez choisir une photo.");
        if (fichier.getSize() > TAILLE_MAX) throw new FichierInvalideException("Fichier trop volumineux (5 Mo maximum).");
        if (!TYPES.contains(fichier.getContentType())) throw new FichierInvalideException("Format refusé : JPG ou PNG uniquement.");
        BufferedImage source;
        try (InputStream in = fichier.getInputStream()) {
            source = ImageIO.read(in);
        } catch (IOException e) {
            throw new FichierInvalideException("Impossible de lire l'image.");
        }
        if (source == null) throw new FichierInvalideException("Le fichier n'est pas une image valide.");
        if (source.getWidth() < 200 || source.getHeight() < 120 || (long) source.getWidth() * source.getHeight() > 25_000_000L) {
            throw new FichierInvalideException("Dimensions non supportées (min 200×120, max 25 mégapixels).");
        }
        return StockageImageService.normaliser(source, 1600);
    }

    /** Enregistre une image publique (visible dans le catalogue) et renvoie son URL, ex. /media/produits/xxxx.jpg. */
    @Override
    public String sauverPublic(MultipartFile fichier, String dossier) {
        if (!DOSSIERS.contains(dossier)) throw new IllegalArgumentException("Dossier inconnu");
        BufferedImage img = valider(fichier);
        String nom = UUID.randomUUID() + ".jpg";
        try {
            Path dir = media.resolve(dossier);
            Files.createDirectories(dir);
            ImageIO.write(img, "jpg", dir.resolve(nom).toFile());
        } catch (IOException e) {
            throw new FichierInvalideException("Erreur lors de l'enregistrement de l'image.");
        }
        return "/media/" + dossier + "/" + nom;
    }

    /** Relit une image publique à partir de son URL /media/... (vide si l'URL n'est pas un fichier local). */
    @Override
    public Optional<byte[]> lirePublic(String url) {
        if (url == null || !url.startsWith("/media/")) return Optional.empty();
        Path f = media.resolve(url.substring("/media/".length())).normalize();
        if (!f.startsWith(media) || !Files.isRegularFile(f)) return Optional.empty();
        try {
            return Optional.of(Files.readAllBytes(f));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<BufferedImage> lireImagePublique(String url) {
        return lirePublic(url).map(b -> {
            try {
                return ImageIO.read(new java.io.ByteArrayInputStream(b));
            } catch (IOException e) {
                return null;
            }
        }).map(i -> StockageImageService.normaliser(i, 1600));
    }

}
