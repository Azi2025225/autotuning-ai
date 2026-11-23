package com.autotuning.service;

import com.autotuning.dto.MoteurForm;
import com.autotuning.dto.ReponseIA;
import com.autotuning.model.ConsultationIA;
import java.util.List;

public interface ConseillerIAService {

    ConsultationIA analyser(String email, MoteurForm form);

    ReponseIA lire(ConsultationIA c);

    ConsultationIA consultation(String email, Long id);

    List<ConsultationIA> historique(String email);

    /** Texte libre (chatbot) : même fournisseur, sans forcer le format JSON. Ne gère pas le mode « mock ». */
    String discuter(String prompt);

    boolean iaReelleConfiguree();
}
