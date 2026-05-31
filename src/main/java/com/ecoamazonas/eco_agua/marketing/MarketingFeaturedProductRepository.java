package com.ecoamazonas.eco_agua.marketing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarketingFeaturedProductRepository extends JpaRepository<MarketingFeaturedProduct, Long> {

    @Query("""
        select distinct item
        from MarketingFeaturedProduct item
        left join fetch item.product
        order by item.priority desc, item.updatedAt desc, item.id desc
        """)
    List<MarketingFeaturedProduct> findAllForAdmin();

    @Query("""
        select distinct item
        from MarketingFeaturedProduct item
        left join fetch item.product
        where item.id = :id
        """)
    Optional<MarketingFeaturedProduct> findByIdForAdmin(@Param("id") Long id);

    @Query("""
        select count(item) > 0
        from MarketingFeaturedProduct item
        where item.product.id = :productId
          and item.status = :status
        """)
    boolean existsByProductIdAndStatus(@Param("productId") Long productId,
                                       @Param("status") MarketingFeaturedProduct.Status status);
}
