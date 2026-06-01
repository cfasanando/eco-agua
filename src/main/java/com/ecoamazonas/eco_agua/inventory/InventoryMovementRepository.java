package com.ecoamazonas.eco_agua.inventory;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.supply.Supply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findByProductOrderByMovementDateDesc(Product product);

    List<InventoryMovement> findByProductOrderByMovementDateAscIdAsc(Product product);

    Optional<InventoryMovement> findTopByProductOrderByMovementDateDescIdDesc(Product product);

    List<InventoryMovement> findBySupplyOrderByMovementDateDesc(Supply supply);

    @Query("""
        select m
        from InventoryMovement m
        left join fetch m.product p
        left join fetch p.category c
        where m.referenceModule = 'PURCHASE'
          and m.product is not null
          and m.movementDate between :start and :end
          and (:productId is null or p.id = :productId)
        order by m.movementDate desc, m.id desc
        """)
    List<InventoryMovement> findProductPurchaseMovements(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("productId") Long productId
    );

    boolean existsByReferenceModuleAndReferenceId(String referenceModule, Long referenceId);
}

