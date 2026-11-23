package com.autotuning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final String googleClientId;
    private final String googleClientSecret;
    private final String googleScopes;
    private final String rememberMeKey;
    private final com.autotuning.service.ConnexionGoogleService connexionGoogleService;
    private final com.autotuning.service.UtilisateurService utilisateurs;

    public SecurityConfig(@Value("${app.google.client-id:}") String googleClientId,
                          @Value("${app.google.client-secret:}") String googleClientSecret,
                          @Value("${app.google.scope:openid,profile,email}") String googleScopes,
                          @Value("${app.security.remember-me-key:}") String rememberMeKey,
                          com.autotuning.service.ConnexionGoogleService connexionGoogleService,
                          com.autotuning.service.UtilisateurService utilisateurs) {
        this.googleClientId = googleClientId;
        this.googleClientSecret = googleClientSecret;
        this.googleScopes = googleScopes;
        this.rememberMeKey = rememberMeKey;
        this.connexionGoogleService = connexionGoogleService;
        this.utilisateurs = utilisateurs;
    }

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        if (googleClientId.isBlank() || googleClientSecret.isBlank()) {
            return registrationId -> null;
        }
        ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(googleClientId).clientSecret(googleClientSecret)
                .scope(googleScopes.split(",")).userNameAttributeName("email").build();
        return new InMemoryClientRegistrationRepository(google);
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler succesConnexion = new SavedRequestAwareAuthenticationSuccessHandler();
        succesConnexion.setDefaultTargetUrl("/");
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/", "/catalogue/**", "/produit/**", "/login", "/inscription",
                                 "/mot-de-passe-oublie", "/reinitialiser-mot-de-passe",
                                 "/css/**", "/js/**", "/img/**", "/media/**", "/modeles", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/avis").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(f -> f
                .loginPage("/login")
                .usernameParameter("email")
                .defaultSuccessUrl("/", false)
                .successHandler((request, response, authentication) -> {
                    utilisateurs.reinitialiserEchecsConnexion(authentication.getName());
                    succesConnexion.onAuthenticationSuccess(request, response, authentication);
                })
                .failureHandler((request, response, exception) -> {
                    if (exception instanceof BadCredentialsException) {
                        utilisateurs.enregistrerEchecConnexion(request.getParameter("email"));
                    }
                    response.sendRedirect("/login?erreur");
                }));
        if (!rememberMeKey.isBlank()) {
            http.rememberMe(r -> r.key(rememberMeKey).tokenValiditySeconds(14 * 24 * 60 * 60)
                    .rememberMeParameter("remember-me"));
        }
        http.oauth2Login(o -> o.loginPage("/login")
                    .userInfoEndpoint(u -> u.oidcUserService(connexionGoogleService)))
            .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/?deconnecte").deleteCookies("JSESSIONID"))
            .exceptionHandling(e -> e.accessDeniedPage("/acces-refuse"));
        return http.build();
    }
}
