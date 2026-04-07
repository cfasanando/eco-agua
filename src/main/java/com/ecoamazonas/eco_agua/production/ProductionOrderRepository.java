package com.ecoamazonas.eco_agua.production;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {

    @Query("""
            select distinct p
            from ProductionOrder p
            left join fetch p.product pr
            where (:status is null or p.status = :status)
              and p.productionDate between :startDate and :endDate
            order by p.productionDate desc, p.id desc
            """)
    List<ProductionOrder> findByDateRangeAndStatus(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") ProductionStatus status
    );

    @Query("""
            select distinct p
            from ProductionOrder p
            left join fetch p.product pr
            left join fetch p.supplies s
            left join fetch s.supply su
            where p.id = :id
            """)
    Optional<ProductionOrder> findDetailedById(@Param("id") Long id);
}
