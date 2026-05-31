package com.ecoamazonas.eco_agua.marketing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarketingPublicationPlanRepository extends JpaRepository<MarketingPublicationPlanItem, Long> {

    @Query("""
        select item
        from MarketingPublicationPlanItem item
        order by item.publicationDate asc, item.updatedAt desc, item.id desc
        """)
    List<MarketingPublicationPlanItem> findAllForAdmin();

    @Query("""
        select distinct item
        from MarketingPublicationPlanItem item
        left join fetch item.idea
        left join fetch item.campaign
        left join fetch item.strategy
        left join fetch item.product
        where item.id = :id
        """)
    Optional<MarketingPublicationPlanItem> findByIdForAdmin(@Param("id") Long id);
}
