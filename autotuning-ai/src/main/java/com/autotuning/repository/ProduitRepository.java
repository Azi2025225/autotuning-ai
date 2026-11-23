package com.autotuning.repository;

import com.autotuning.model.Produit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;

public interface ProduitRepository extends JpaRepository<Produit, Long> {

    @Query("""
            select p from Produit p
            where (:q is null or lower(p.nom) like lower(concat('%', :q, '%'))
                   or lower(p.description) like lower(concat('%', :q, '%')))
              and (:categorieId is null or p.categorie.id = :categorieId)
              and (:marque is null or lower(p.compatibilite) like lower(concat('%', :marque, '%')))
              and (:prixMin is null or p.prix >= :prixMin)
              and (:prixMax is null or p.prix <= :prixMax)
            """)
    List<Produit> rechercher(@Param("q") String q, @Param("categorieId") Long categorieId,
                             @Param("marque") String marque, @Param("prixMin") BigDecimal prixMin,
                             @Param("prixMax") BigDecimal prixMax, Sort sort);

    List<Produit> findTop4ByStockGreaterThanOrderByIdAsc(int stock);
    /** Verrou en écriture : empêche deux commandes simultanées de survendre le même stock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produit p where p.id = :id")
    Optional<Produit> findByIdForUpdate(@Param("id") Long id);

    long countByCategorieId(Long categorieId);
}
