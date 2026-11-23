package com.autotuning.service.impl;

import com.autotuning.service.*;

import com.autotuning.config.WebConfig;
import com.autotuning.exception.FichierInvalideException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StockageImageServiceTest {
    @TempDir Path tmp;

    private StockageImageService service() throws Exception {
        WebConfig cfg = new WebConfig();
        ReflectionTestUtils.setField(cfg, "uploadDir", tmp.toString());
        return new StockageImageServiceImpl(cfg);
    }

    private static byte[] png(int w, int h) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB), "png", bos);
        return bos.toByteArray();
    }

    @Test
    void fichierVideRefuse() throws Exception {
        assertThrows(FichierInvalideException.class,
                () -> service().valider(new MockMultipartFile("photo", "a.png", "image/png", new byte[0])));
    }

    @Test
    void mauvaisTypeRefuse() throws Exception {
        assertThrows(FichierInvalideException.class,
                () -> service().valider(new MockMultipartFile("photo", "a.pdf", "application/pdf", new byte[]{1, 2})));
    }

    @Test
    void fauxPngRefuse() throws Exception {
        assertThrows(FichierInvalideException.class,
                () -> service().valider(new MockMultipartFile("photo", "a.png", "image/png", "pas une image".getBytes())));
    }

    @Test
    void imageTropPetiteRefusee() throws Exception {
        assertThrows(FichierInvalideException.class,
                () -> service().valider(new MockMultipartFile("photo", "a.png", "image/png", png(50, 50))));
    }

    @Test
    void imageValideEstEnregistreeEtRelisible() throws Exception {
        StockageImageService s = service();
        String url = s.sauverPublic(new MockMultipartFile("photo", "voiture.png", "image/png", png(400, 300)), "produits");
        assertTrue(url.startsWith("/media/produits/") && url.endsWith(".jpg"));
        assertTrue(s.lirePublic(url).isPresent());
        assertTrue(s.lireImagePublique(url).isPresent());
    }

    @Test
    void cheminHorsDuDossierMediaIgnore() throws Exception {
        assertTrue(service().lirePublic("/media/../../etc/passwd").isEmpty());
    }

    @Test
    void dossierInconnuRefuse() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> service().sauverPublic(new MockMultipartFile("photo", "a.png", "image/png", png(400, 300)), "../autre"));
    }
}
