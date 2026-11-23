package com.autotuning.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Seul le sous-dossier « media » (photos de produits et de modèles) est public.
 * Les photos privées des clients (personnalisation) ne sont PAS exposées : elles passent par un contrôleur
 * qui vérifie le propriétaire.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public String getUploadDir() { return uploadDir; }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path media = Path.of(uploadDir).toAbsolutePath().normalize().resolve("media");
        try { Files.createDirectories(media); } catch (Exception ignored) { }
        registry.addResourceHandler("/media/**").addResourceLocations(media.toUri().toString());
    }
}
