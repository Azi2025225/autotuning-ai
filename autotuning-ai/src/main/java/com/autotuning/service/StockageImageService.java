package com.autotuning.service;

import com.autotuning.exception.FichierInvalideException;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public interface StockageImageService {

    static final long TAILLE_MAX = 5L * 1024 * 1024;

    Path getRacine();

    /** Contrôle type, taille, contenu réellement décodable et dimensions ; renvoie l'image normalisée (RVB, max 1600 px). */
    BufferedImage valider(MultipartFile fichier);

    /** Enregistre une image publique (visible dans le catalogue) et renvoie son URL, ex. /media/produits/xxxx.jpg. */
    String sauverPublic(MultipartFile fichier, String dossier);

    /** Relit une image publique à partir de son URL /media/... (vide si l'URL n'est pas un fichier local). */
    Optional<byte[]> lirePublic(String url);

    Optional<BufferedImage> lireImagePublique(String url);

    static BufferedImage normaliser(BufferedImage src, int max) {
        int w = src.getWidth(), h = src.getHeight();
        double echelle = Math.min(1.0, (double) max / Math.max(w, h));
        int nw = Math.max(1, (int) (w * echelle)), nh = Math.max(1, (int) (h * echelle));
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, Color.WHITE, null);
        g.dispose();
        return out;
    }

    static byte[] enJpeg(BufferedImage img) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "jpg", bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new FichierInvalideException("Erreur de traitement de l'image.");
        }
    }
}
