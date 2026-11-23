package com.autotuning.service;

public interface ReinitialisationMotDePasseService {

    void demander(String email);

    boolean reinitialiser(String token, String nouveauMotDePasse);

    boolean tokenValide(String token);
}
