package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.model.PasswordResetRequest;
import com.autotuning.model.PasswordResetToken;
import com.autotuning.model.Utilisateur;
import com.autotuning.repository.PasswordResetRequestRepository;
import com.autotuning.repository.PasswordResetTokenRepository;
import com.autotuning.repository.UtilisateurRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class ReinitialisationMotDePasseServiceImpl implements ReinitialisationMotDePasseService {
    private static final Logger log = LoggerFactory.getLogger(ReinitialisationMotDePasseServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UtilisateurRepository utilisateurs;
    private final PasswordResetTokenRepository tokens;
    private final PasswordResetRequestRepository demandes;
    private final PasswordEncoder encoder;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String mailHost;
    private final String mailUser;
    private final String applicationUrl;
    private final Environment environment;

    public ReinitialisationMotDePasseServiceImpl(UtilisateurRepository utilisateurs, PasswordResetTokenRepository tokens,
            PasswordResetRequestRepository demandes, PasswordEncoder encoder, ObjectProvider<JavaMailSender> mailSender,
            @Value("${spring.mail.host:}") String mailHost, @Value("${spring.mail.username:}") String mailUser,
            @Value("${app.url:http://localhost:8080}") String applicationUrl, Environment environment) {
        this.utilisateurs = utilisateurs;
        this.tokens = tokens;
        this.demandes = demandes;
        this.encoder = encoder;
        this.mailSender = mailSender;
        this.mailHost = mailHost;
        this.mailUser = mailUser;
        this.applicationUrl = applicationUrl;
        this.environment = environment;
    }

    @Transactional
    @Override
    public synchronized void demander(String email) {
        String normalise = email == null ? "" : email.trim().toLowerCase();
        String empreinteEmail = hacher(normalise);
        Instant maintenant = Instant.now();
        if (demandes.countByEmailHashAndDateDemandeAfter(empreinteEmail, maintenant.minus(1, ChronoUnit.HOURS)) >= 3) return;

        PasswordResetRequest demande = new PasswordResetRequest();
        demande.setEmailHash(empreinteEmail);
        demande.setDateDemande(maintenant);
        demandes.save(demande);

        utilisateurs.findByEmailIgnoreCase(normalise).ifPresent(utilisateur -> envoyerLien(utilisateur, maintenant));
    }

    @Transactional
    @Override
    public boolean reinitialiser(String token, String nouveauMotDePasse) {
        PasswordResetToken reset = tokens.trouverEtVerrouillerParHash(hacher(token)).orElse(null);
        Instant maintenant = Instant.now();
        if (reset == null || reset.isUtilise() || !reset.getExpiration().isAfter(maintenant)) return false;

        Utilisateur utilisateur = reset.getUtilisateur();
        utilisateur.setMotDePasse(encoder.encode(nouveauMotDePasse));
        tokens.invaliderPourUtilisateur(utilisateur.getId());
        reset.setUtilise(true);
        return true;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean tokenValide(String token) {
        if (token == null || token.isBlank()) return false;
        return tokens.findByTokenHash(hacher(token))
                .filter(t -> !t.isUtilise() && t.getExpiration().isAfter(Instant.now()))
                .isPresent();
    }

    private void envoyerLien(Utilisateur utilisateur, Instant maintenant) {
        byte[] aleatoire = new byte[32];
        RANDOM.nextBytes(aleatoire);
        String tokenClair = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatoire);
        PasswordResetToken reset = new PasswordResetToken();
        reset.setUtilisateur(utilisateur);
        reset.setTokenHash(hacher(tokenClair));
        reset.setDateCreation(maintenant);
        reset.setExpiration(maintenant.plus(30, ChronoUnit.MINUTES));
        reset.setUtilise(false);
        tokens.save(reset);

        String lien = UriComponentsBuilder.fromUriString(applicationUrl)
                .path("/reinitialiser-mot-de-passe").queryParam("token", tokenClair).build().toUriString();
        if (mailHost.isBlank() || mailUser.isBlank() || mailSender.getIfAvailable() == null) {
            if (environment.acceptsProfiles(Profiles.of("dev", "h2"))) {
                log.info("Lien de réinitialisation (mode développement) pour {} : {}", utilisateur.getEmail(), lien);
            } else {
                log.error("SMTP non configuré : impossible d'envoyer le lien de réinitialisation pour {}", utilisateur.getEmail());
            }
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(utilisateur.getEmail());
        message.setSubject("Réinitialisation de votre mot de passe AutoTuning AI");
        message.setText("Pour choisir un nouveau mot de passe, ouvrez ce lien (valable 30 minutes) :\n" + lien);
        try {
            mailSender.getObject().send(message);
        } catch (MailException e) {
            log.error("Échec de l'envoi du lien de réinitialisation pour {}", utilisateur.getEmail(), e);
        }
    }

    private static String hacher(String valeur) {
        try {
            byte[] empreinte = MessageDigest.getInstance("SHA-256").digest(valeur.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(empreinte);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 n'est pas disponible", e);
        }
    }
}
