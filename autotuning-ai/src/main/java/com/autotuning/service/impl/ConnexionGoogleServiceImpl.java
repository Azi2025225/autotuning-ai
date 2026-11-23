package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.model.Fournisseur;
import com.autotuning.model.Role;
import com.autotuning.model.Utilisateur;
import com.autotuning.repository.UtilisateurRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

@Service
public class ConnexionGoogleServiceImpl implements ConnexionGoogleService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UtilisateurRepository utilisateurs;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;
    private final OidcUserService oidcUserService;

    @Autowired
    public ConnexionGoogleServiceImpl(UtilisateurRepository utilisateurs,
                                  org.springframework.security.crypto.password.PasswordEncoder encoder) {
        this(utilisateurs, encoder, new OidcUserService());
    }

    ConnexionGoogleServiceImpl(UtilisateurRepository utilisateurs,
                           org.springframework.security.crypto.password.PasswordEncoder encoder,
                           OidcUserService oidcUserService) {
        this.utilisateurs = utilisateurs;
        this.encoder = encoder;
        this.oidcUserService = oidcUserService;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest request) {
        OidcUser google = oidcUserService.loadUser(request);
        Boolean emailVerifie = google.getAttribute("email_verified");
        String email = google.getAttribute("email");
        String googleId = google.getSubject();
        if (!Boolean.TRUE.equals(emailVerifie) || email == null || email.isBlank()) {
            throw erreur("L'adresse email Google doit être vérifiée.");
        }

        utilisateurs.findByGoogleId(googleId).ifPresent(existant -> {
            if (!existant.getEmail().equalsIgnoreCase(email.trim())) {
                throw erreur("Cet identifiant Google est déjà associé à un autre compte.");
            }
        });
        Utilisateur utilisateur = utilisateurs.findByEmailIgnoreCase(email.trim())
                .orElseGet(() -> creerUtilisateur(google, email, googleId));
        if (!utilisateur.isActif()) throw erreur("Ce compte est désactivé.");
        if (utilisateur.getGoogleId() != null && !utilisateur.getGoogleId().equals(googleId)) {
            throw erreur("Ce compte Google est déjà associé à un autre identifiant.");
        }
        utilisateur.setFournisseur(Fournisseur.GOOGLE);
        utilisateur.setGoogleId(googleId);
        utilisateurs.save(utilisateur);

        List<GrantedAuthority> authorities = new ArrayList<>(google.getAuthorities());
        authorities.removeIf(authority -> authority.getAuthority().startsWith("ROLE_"));
        authorities.add(new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name()));
        return new EmailOidcUser(authorities, google, utilisateur.getEmail());
    }

    private Utilisateur creerUtilisateur(OidcUser google, String email, String googleId) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email.trim().toLowerCase());
        utilisateur.setNom(valeurOu(google.getAttribute("family_name"), "Google"));
        utilisateur.setPrenom(valeurOu(google.getAttribute("given_name"), "Utilisateur"));
        byte[] aleatoire = new byte[32];
        RANDOM.nextBytes(aleatoire);
        utilisateur.setMotDePasse(encoder.encode(Base64.getUrlEncoder().withoutPadding().encodeToString(aleatoire)));
        utilisateur.setRole(Role.CLIENT);
        utilisateur.setFournisseur(Fournisseur.GOOGLE);
        utilisateur.setGoogleId(googleId);
        return utilisateurs.save(utilisateur);
    }

    private static String valeurOu(String valeur, String valeurDefaut) {
        return valeur == null || valeur.isBlank() ? valeurDefaut : valeur;
    }

    private static OAuth2AuthenticationException erreur(String description) {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info", description, null));
    }

    private static final class EmailOidcUser implements OidcUser {
        private final Collection<? extends GrantedAuthority> authorities;
        private final OidcUser delegate;
        private final String email;

        private EmailOidcUser(Collection<? extends GrantedAuthority> authorities, OidcUser delegate, String email) {
            this.authorities = authorities;
            this.delegate = delegate;
            this.email = email;
        }

        @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
        @Override public java.util.Map<String, Object> getClaims() { return delegate.getClaims(); }
        @Override public java.util.Map<String, Object> getAttributes() { return delegate.getAttributes(); }
        @Override public OidcIdToken getIdToken() { return delegate.getIdToken(); }
        @Override public OidcUserInfo getUserInfo() { return delegate.getUserInfo(); }
        @Override
        public String getName() {
            return email;
        }
    }
}
