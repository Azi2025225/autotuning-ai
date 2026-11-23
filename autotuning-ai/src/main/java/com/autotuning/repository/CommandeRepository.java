package com.autotuning.repository;

import com.autotuning.model.Commande;
import com.autotuning.model.StatutCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CommandeRepository extends JpaRepository<Commande, Long> {
    List<Commande> findByUtilisateurIdOrderByDateCommandeDesc(Long utilisateurId);
    Optional<Commande> findByIdAndUtilisateurId(Long id, Long utilisateurId);
    List<Commande> findAllByOrderByDateCommandeDesc();
    List<Commande> findTop5ByOrderByDateCommandeDesc();
    boolean existsByLignesProduitId(Long produitId);

    @Query("select coalesce(sum(c.total), 0) from Commande c where c.statut <> :exclu")
    BigDecimal chiffreAffaires(StatutCommande exclu);

    @Query("""
            select l.produit.nom, sum(l.quantite) from LigneCommande l
            where l.commande.statut <> com.autotuning.model.StatutCommande.ANNULEE
            group by l.produit.nom order by sum(l.quantite) desc
            """)
    List<Object[]> meilleuresVentes();
}
