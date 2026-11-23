package com.autotuning;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.autotuning.model.PasswordResetToken;
import com.autotuning.model.Role;
import com.autotuning.model.Utilisateur;
import com.autotuning.repository.PasswordResetTokenRepository;
import com.autotuning.repository.PasswordResetRequestRepository;
import com.autotuning.repository.UtilisateurRepository;
import com.autotuning.service.UtilisateurService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class SecuriteTest {
    @Autowired MockMvc mvc;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired PasswordEncoder encoder;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordResetRequestRepository resetRequests;
    @Autowired UtilisateurService utilisateurService;

    @Test
    void pageConnexionEstAccessible() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("login"));
    }

    @Test
    void pageAvisEstPubliqueEtSaffiche() throws Exception {
        mvc.perform(get("/avis")).andExpect(status().isOk())
                .andExpect(view().name("avis")).andExpect(model().attributeExists("avis", "form"));
    }

    @Test
    void clientPeutPublierUnAvis() throws Exception {
        Utilisateur client = creerUtilisateur("avis-client@example.com", true);
        mvc.perform(post("/avis").with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(client.getEmail()).roles("CLIENT"))
                        .param("note", "5").param("commentaire", "Très bonne expérience, merci !"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/avis"));
        mvc.perform(get("/avis")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Très bonne expérience, merci !")));
    }

    @Test
    void publicationAvisNecessiteUneConnexion() throws Exception {
        mvc.perform(post("/avis").with(csrf()).param("note", "5")
                        .param("commentaire", "Très bonne expérience, merci !"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void cataloguePublic() throws Exception {
        mvc.perform(get("/catalogue")).andExpect(status().isOk());
    }

    @Test
    void panierExigeUneConnexion() throws Exception {
        mvc.perform(get("/panier")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void clientRefuseSurPageAdmin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@autotuning.tn", roles = "ADMIN")
    void adminAccedeAuDashboard() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk());
    }

    @Test
    void inscriptionPuisEmailDejaUtilise() throws Exception {
        var req = post("/inscription").with(csrf())
                .param("nom", "Ben Ali").param("prenom", "Ahmed").param("email", "doublon@example.com")
                .param("telephone", "20123456").param("motDePasse", "motdepasse1").param("confirmation", "motdepasse1");
        mvc.perform(req).andExpect(status().is3xxRedirection());
        mvc.perform(req).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "email"));
    }

    @Test
    void postSansCsrfRefuse() throws Exception {
        mvc.perform(post("/inscription").param("email", "x@y.z")).andExpect(status().isForbidden());
    }

    @Test
    void connexionCorrecteEtIncorrecte() throws Exception {
        Utilisateur user = creerUtilisateur("connexion@example.com", true);
        mvc.perform(post("/login").with(csrf()).param("email", user.getEmail()).param("password", "Motdepasse123"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/login").with(csrf()).param("email", user.getEmail()).param("password", "incorrect"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?erreur"));
    }

    @Test
    void compteInactifRefuseLaConnexion() throws Exception {
        Utilisateur user = creerUtilisateur("inactif@example.com", false);
        mvc.perform(post("/login").with(csrf()).param("email", user.getEmail()).param("password", "Motdepasse123"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?erreur"));
    }

    @Test
    void verrouillageApresCinqEchecs() throws Exception {
        Utilisateur user = creerUtilisateur("verrou@example.com", true);
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/login").with(csrf()).param("email", user.getEmail()).param("password", "faux"))
                    .andExpect(status().is3xxRedirection());
        }
        Utilisateur verrouille = utilisateurs.findByEmailIgnoreCase(user.getEmail()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(verrouille.estVerrouille());
        mvc.perform(post("/login").with(csrf()).param("email", user.getEmail()).param("password", "Motdepasse123"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?erreur"));
    }

    @Test
    void motDePasseOubliePuisTokenValideUtiliseUneSeuleFois() throws Exception {
        Utilisateur user = creerUtilisateur("reset@example.com", true);
        String token = "token-valide-test";
        String secondToken = "second-token-test";
        creerToken(user, token, Instant.now().plus(30, ChronoUnit.MINUTES), false);
        creerToken(user, secondToken, Instant.now().plus(30, ChronoUnit.MINUTES), false);
        mvc.perform(post("/reinitialiser-mot-de-passe").with(csrf()).param("token", token)
                        .param("motDePasse", "Nouveau123").param("confirmation", "Nouveau123"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?motDePasseReinitialise"));
        org.junit.jupiter.api.Assertions.assertTrue(encoder.matches("Nouveau123",
                utilisateurs.findByEmailIgnoreCase(user.getEmail()).orElseThrow().getMotDePasse()));
        mvc.perform(post("/reinitialiser-mot-de-passe").with(csrf()).param("token", token)
                        .param("motDePasse", "Autre1234").param("confirmation", "Autre1234"))
                .andExpect(status().isOk()).andExpect(model().attribute("tokenValide", false));
        mvc.perform(get("/reinitialiser-mot-de-passe").param("token", secondToken))
                .andExpect(status().isOk()).andExpect(model().attribute("tokenValide", false));
    }

    @Test
    void tokenExpireRefuseLaReinitialisation() throws Exception {
        Utilisateur user = creerUtilisateur("expire@example.com", true);
        String token = "token-expire-test";
        creerToken(user, token, Instant.now().minus(1, ChronoUnit.MINUTES), false);
        mvc.perform(post("/reinitialiser-mot-de-passe").with(csrf()).param("token", token)
                        .param("motDePasse", "Nouveau123").param("confirmation", "Nouveau123"))
                .andExpect(status().isOk()).andExpect(model().attribute("tokenValide", false));
    }

    @Test
    void reponseMotDePasseOublieNeRevelePasSiEmailExiste() throws Exception {
        mvc.perform(post("/mot-de-passe-oublie").with(csrf()).param("email", "inconnu@example.com"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("message",
                        "Si cette adresse correspond à un compte, un lien de réinitialisation sera envoyé."));
    }

    @Test
    void demandesMotDePasseOublieLimiteesATroisParHeure() throws Exception {
        String email = "limite@example.com";
        for (int i = 0; i < 4; i++) {
            mvc.perform(post("/mot-de-passe-oublie").with(csrf()).param("email", email))
                    .andExpect(status().is3xxRedirection());
        }
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(email.getBytes(StandardCharsets.UTF_8)));
        org.junit.jupiter.api.Assertions.assertEquals(3,
                resetRequests.countByEmailHashAndDateDemandeAfter(hash, Instant.now().minus(1, ChronoUnit.HOURS)));
    }

    @Test
    void impossibleDeRetirerLeDernierRoleAdministrateurActif() {
        Utilisateur admin = utilisateurs.findByEmailIgnoreCase("admin@autotuning.tn").orElseThrow();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> utilisateurService.mettreAJourAcces(admin.getId(), Role.CLIENT, true, "autre@example.com"));
    }

    @Test
    void administrateurNePeutPasDesactiverSonCompte() {
        Utilisateur admin = utilisateurs.findByEmailIgnoreCase("admin@autotuning.tn").orElseThrow();
        Utilisateur secondAdmin = creerUtilisateur("admin-secondaire@example.com", true);
        secondAdmin.setRole(Role.ADMIN);
        utilisateurs.save(secondAdmin);
        try {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                    () -> utilisateurService.mettreAJourAcces(admin.getId(), Role.ADMIN, false, admin.getEmail()));
        } finally {
            utilisateurs.delete(secondAdmin);
        }
    }

    private Utilisateur creerUtilisateur(String email, boolean actif) {
        Utilisateur user = new Utilisateur();
        user.setNom("Test");
        user.setPrenom("Auth");
        user.setEmail(email);
        user.setMotDePasse(encoder.encode("Motdepasse123"));
        user.setRole(Role.CLIENT);
        user.setActif(actif);
        return utilisateurs.save(user);
    }

    private void creerToken(Utilisateur user, String tokenClair, Instant expiration, boolean utilise) throws Exception {
        byte[] empreinte = MessageDigest.getInstance("SHA-256").digest(tokenClair.getBytes(StandardCharsets.UTF_8));
        PasswordResetToken token = new PasswordResetToken();
        token.setUtilisateur(user);
        token.setTokenHash(HexFormat.of().formatHex(empreinte));
        token.setExpiration(expiration);
        token.setDateCreation(Instant.now());
        token.setUtilise(utilise);
        tokens.save(token);
    }
}
