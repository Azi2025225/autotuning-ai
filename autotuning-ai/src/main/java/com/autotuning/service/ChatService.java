package com.autotuning.service;

import java.util.*;

public interface ChatService {

    record Tour(String role, String content) { }

    record Reponse(String texte, boolean local) { }

    Reponse repondre(String email, String message, List<Tour> historique);
}
