package com.autotuning.service.impl;

import com.autotuning.service.*;

import com.autotuning.model.Fournisseur;
import com.autotuning.model.Role;
import com.autotuning.model.Utilisateur;
import com.autotuning.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("h2")
class ConnexionGoogleServiceTest {
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired PasswordEncoder encoder;

    @Test
    void creeUnCompteGoogleAvecEmailEtRoleClient() {
        OidcUserRequest request = mock(OidcUserRequest.class);
        OidcUser google = oidcUser("google-premiere-connexion@example.com", true, "google-uid-1");
        OidcUserService delegate = mock(OidcUserService.class);
        when(delegate.loadUser(request)).thenReturn(google);

        OidcUser principal = new ConnexionGoogleServiceImpl(utilisateurs, encoder, delegate).loadUser(request);

        Utilisateur user = utilisateurs.findByEmailIgnoreCase("google-premiere-connexion@example.com").orElseThrow();
        assertEquals("google-premiere-connexion@example.com", principal.getName());
        assertEquals(Role.CLIENT, user.getRole());
        assertEquals(Fournisseur.GOOGLE, user.getFournisseur());
        assertEquals("google-uid-1", user.getGoogleId());
        assertFalse(encoder.matches("not-the-google-password", user.getMotDePasse()));
        assertTrue(principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT")));
    }

    @Test
    void refuseUnEmailGoogleNonVerifie() {
        OidcUserRequest request = mock(OidcUserRequest.class);
        OidcUserService delegate = mock(OidcUserService.class);
        OidcUser google = oidcUser("google-non-verifie@example.com", false, "google-uid-2");
        when(delegate.loadUser(request)).thenReturn(google);

        assertThrows(OAuth2AuthenticationException.class,
                () -> new ConnexionGoogleServiceImpl(utilisateurs, encoder, delegate).loadUser(request));
        assertFalse(utilisateurs.existsByEmailIgnoreCase("google-non-verifie@example.com"));
    }

    @Test
    void conserveLeRoleAdminDuCompteAssocie() {
        Utilisateur admin = new Utilisateur();
        admin.setNom("Admin");
        admin.setPrenom("Google");
        admin.setEmail("google-admin@example.com");
        admin.setMotDePasse(encoder.encode("local-password"));
        admin.setRole(Role.ADMIN);
        admin = utilisateurs.save(admin);

        OidcUserRequest request = mock(OidcUserRequest.class);
        OidcUserService delegate = mock(OidcUserService.class);
        OidcUser google = oidcUser(admin.getEmail(), true, "google-admin-id");
        when(delegate.loadUser(request)).thenReturn(google);
        OidcUser principal = new ConnexionGoogleServiceImpl(utilisateurs, encoder, delegate).loadUser(request);

        assertEquals(Role.ADMIN, utilisateurs.findByEmailIgnoreCase(admin.getEmail()).orElseThrow().getRole());
        assertTrue(principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertEquals(admin.getEmail(), principal.getName());
    }

    @Test
    void refuseUnCompteGoogleDesactive() {
        Utilisateur user = new Utilisateur();
        user.setNom("Inactif");
        user.setPrenom("Google");
        user.setEmail("google-inactif@example.com");
        user.setMotDePasse(encoder.encode("motdepasse1"));
        user.setActif(false);
        utilisateurs.save(user);

        OidcUserRequest request = mock(OidcUserRequest.class);
        OidcUserService delegate = mock(OidcUserService.class);
        OidcUser google = oidcUser(user.getEmail(), true, "google-inactive-id");
        when(delegate.loadUser(request)).thenReturn(google);

        assertThrows(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class,
                () -> new ConnexionGoogleServiceImpl(utilisateurs, encoder, delegate).loadUser(request));
    }

    private OidcUser oidcUser(String email, boolean emailVerified, String subject) {
        Instant maintenant = Instant.now();
        Map<String, Object> claims = Map.of("email", email, "email_verified", emailVerified,
                "given_name", "Prénom", "family_name", "Nom", "sub", subject,
                "iss", "https://accounts.google.com", "aud", List.of("test-client"),
                "iat", maintenant, "exp", maintenant.plusSeconds(300));
        OidcIdToken idToken = new OidcIdToken("test-token", maintenant, maintenant.plusSeconds(300), claims);
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken, "email");
    }
}
