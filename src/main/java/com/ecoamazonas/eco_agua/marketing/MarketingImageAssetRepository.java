package com.ecoamazonas.eco_agua.marketing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarketingImageAssetRepository extends JpaRepository<MarketingImageAsset, Long> {

    @Query("""
        select distinct asset
        from MarketingImageAsset asset
        left join fetch asset.product
        left join fetch asset.campaign
        left join fetch asset.promotion
        order by asset.status asc, asset.updatedAt desc, asset.createdAt desc, asset.id desc
        """)
    List<MarketingImageAsset> findAllForAdmin();

    @Query("""
        select distinct asset
        from MarketingImageAsset asset
        left join fetch asset.product
        left join fetch asset.campaign
        left join fetch asset.promotion
        where asset.id = :id
        """)
    Optional<MarketingImageAsset> findByIdForAdmin(@Param("id") Long id);
}
