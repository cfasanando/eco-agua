package com.ecoamazonas.eco_agua.marketing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MarketingStrategyRepository extends JpaRepository<MarketingStrategy, Long> {

    @Query("""
        select distinct strategy
        from MarketingStrategy strategy
        left join fetch strategy.product
        left join fetch strategy.promotion
        order by strategy.updatedAt desc, strategy.createdAt desc, strategy.id desc
        """)
    List<MarketingStrategy> findAllForAdmin();

    List<MarketingStrategy> findTop5ByStatusOrderByUpdatedAtDescIdDesc(MarketingStrategy.Status status);
}
