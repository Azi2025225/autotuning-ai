package com.autotuning.service;

import com.autotuning.model.AvisClient;
import java.util.List;

public interface AvisClientService {

    List<AvisClient> tous();

    Double noteMoyenne();

    boolean aDejaDonneUnAvis(String email);

    void ajouter(String email, int note, String commentaire);
}
