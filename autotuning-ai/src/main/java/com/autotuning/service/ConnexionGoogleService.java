package com.autotuning.service;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;

public interface ConnexionGoogleService extends OAuth2UserService<OidcUserRequest, OidcUser> {

}
