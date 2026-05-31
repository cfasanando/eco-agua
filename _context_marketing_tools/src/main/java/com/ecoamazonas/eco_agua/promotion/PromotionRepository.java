package com.ecoamazonas.eco_agua.promotion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findByEnabledTrueOrderByNameAsc();

    @Query("""
        select distinct p
        from Promotion p
        left join p.clients c
        where p.enabled = true
          and (:clientId is null or c.id = :clientId or c.id is null)
          and (p.startDate is null or p.startDate <= :date)
          and (p.endDate is null or p.endDate >= :date)
        """)
    List<Promotion> findApplicableForClientAndDate(
            @Param("clientId") Long clientId,
            @Param("date") LocalDate date
    );

    @Query("""
        select p
        from Promotion p
        where p.enabled = true
          and (p.startDate is null or p.startDate <= :today)
          and (p.endDate is null or p.endDate >= :today)
        order by p.promoNumber desc, p.startDate desc
        """)
    List<Promotion> findActiveForPublic(@Param("today") LocalDate today);
    
    Promotion findTopByNameOrderByCreatedAtDesc(String name);
}
