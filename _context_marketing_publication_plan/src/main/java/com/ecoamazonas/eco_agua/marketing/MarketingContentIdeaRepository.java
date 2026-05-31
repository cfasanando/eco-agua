package com.ecoamazonas.eco_agua.marketing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarketingContentIdeaRepository extends JpaRepository<MarketingContentIdea, Long> {

    @Query("""
        select distinct idea
        from MarketingContentIdea idea
        left join fetch idea.product
        left join fetch idea.campaign
        left join fetch idea.strategy
        order by idea.suggestedDate asc, idea.updatedAt desc, idea.id desc
        """)
    List<MarketingContentIdea> findAllForAdmin();

    @Query("""
        select distinct idea
        from MarketingContentIdea idea
        left join fetch idea.product
        left join fetch idea.campaign
        left join fetch idea.strategy
        where idea.id = :id
        """)
    Optional<MarketingContentIdea> findByIdForAdmin(@Param("id") Long id);
}
