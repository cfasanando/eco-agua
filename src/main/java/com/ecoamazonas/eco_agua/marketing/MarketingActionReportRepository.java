package com.ecoamazonas.eco_agua.marketing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarketingActionReportRepository extends JpaRepository<MarketingActionReportItem, Long> {

    @Query("""
        select item
        from MarketingActionReportItem item
        order by item.actionDate desc, item.updatedAt desc, item.id desc
        """)
    List<MarketingActionReportItem> findAllForAdmin();

    @Query("""
        select distinct item
        from MarketingActionReportItem item
        left join fetch item.campaign
        left join fetch item.publicationPlan
        left join fetch item.idea
        left join fetch item.product
        where item.id = :id
        """)
    Optional<MarketingActionReportItem> findByIdForAdmin(@Param("id") Long id);
}
