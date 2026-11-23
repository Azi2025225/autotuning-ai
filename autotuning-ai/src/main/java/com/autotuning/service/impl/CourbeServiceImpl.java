package com.autotuning.service.impl;

import com.autotuning.service.*;
import com.autotuning.dto.ReponseIA;
import com.autotuning.model.MotorisationReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Courbes puissance/couple ILLUSTRATIVES : la forme est modélisée, mais les valeurs de pointe (ch, Nm) proviennent
 * exclusivement de la référence publiée. Ce ne sont pas des mesures au banc.
 */
@Service
public class CourbeServiceImpl implements CourbeService {
    private static final double KW_VERS_CH = 1.35962;

    @Override
    public ReponseIA.Courbes generer(MotorisationReference r) {
        boolean diesel = r.getCarburant().toLowerCase().startsWith("d");
        int c1 = r.getRegimeCoupleMin() != null ? r.getRegimeCoupleMin() : diesel ? 1750 : 1600;
        int c2 = r.getRegimeCoupleMax() != null ? r.getRegimeCoupleMax() : diesel ? 2750 : 4500;
        int max = r.getRegimeMax() != null ? r.getRegimeMax() : diesel ? 4600 : 6500;

        List<Integer> regimes = new ArrayList<>();
        for (int rpm = 1000; rpm <= max; rpm += 250) regimes.add(rpm);

        List<ReponseIA.Serie> series = new ArrayList<>();
        series.add(serie("Origine", r.getPuissanceOrigine(), r.getCoupleOrigine(), regimes, c1, c2, max));
        for (int s = 1; s <= 3; s++) {
            if (r.puissance(s) != null && r.couple(s) != null) {
                series.add(serie("Stage " + s, r.puissance(s), r.couple(s), regimes, c1, c2, max));
            }
        }
        return new ReponseIA.Courbes(regimes, series);
    }

    static ReponseIA.Serie serie(String nom, int pic, int coupleMax, List<Integer> regimes, int c1, int c2, int max) {
        List<Double> couple = new ArrayList<>();
        List<Double> puissanceBrute = new ArrayList<>();
        double pMax = 0;
        for (int rpm : regimes) {
            double t = coupleMax * forme(rpm, c1, c2, max);
            double p = t * rpm / 9549.0 * KW_VERS_CH;
            couple.add(t);
            puissanceBrute.add(p);
            pMax = Math.max(pMax, p);
        }
        double k = pMax > 0 ? pic / pMax : 1; // la puissance maximale tracée = la valeur publiée
        List<Double> puissance = new ArrayList<>();
        for (double p : puissanceBrute) puissance.add(Math.round(p * k * 10) / 10.0);
        List<Double> coupleArrondi = couple.stream().map(t -> (double) Math.round(t)).toList();
        return new ReponseIA.Serie(nom, puissance, coupleArrondi);
    }

    private static double forme(int rpm, int c1, int c2, int max) {
        if (rpm < c1) {
            double x = Math.max(0, (rpm - 1000.0) / Math.max(1, c1 - 1000.0));
            return 0.45 + 0.55 * x * x * (3 - 2 * x);
        }
        if (rpm <= c2) return 1.0;
        double x = Math.min(1.0, (rpm - c2) / (double) Math.max(1, max - c2));
        return 1.0 - 0.30 * Math.pow(x, 1.3);
    }
}
