package com.ecoamazonas.eco_agua.marketing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarketingCampaignCalendarRepository extends JpaRepository<MarketingCampaignCalendarItem, Long> {

    @Query("""
        select distinct campaign
        from MarketingCampaignCalendarItem campaign
        left join fetch campaign.product
        left join fetch campaign.promotion
        left join fetch campaign.strategy
        order by campaign.startDate desc, campaign.updatedAt desc, campaign.id desc
        """)
    List<MarketingCampaignCalendarItem> findAllForAdmin();

    @Query("""
        select distinct campaign
        from MarketingCampaignCalendarItem campaign
        left join fetch campaign.product
        left join fetch campaign.promotion
        left join fetch campaign.strategy
        where campaign.id = :id
        """)
    Optional<MarketingCampaignCalendarItem> findByIdForAdmin(@Param("id") Long id);
}
