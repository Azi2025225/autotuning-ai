package com.autotuning.service;

import com.autotuning.dto.ReponseIA;
import com.autotuning.model.MotorisationReference;

public interface CourbeService {

    ReponseIA.Courbes generer(MotorisationReference r);
}
