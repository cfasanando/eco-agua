package com.ecoamazonas.eco_agua.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface SaleOrderItemRepository extends JpaRepository<SaleOrderItem, Long> {

    /**
     * Sums delivered/commercial quantity for a product in a date range.
     * Only PAID and CREDIT orders are included.
     */
    @Query("""
        select coalesce(sum(i.quantity), 0)
        from SaleOrderItem i
        where i.product.id = :productId
          and i.order.orderDate between :start and :end
          and i.order.status in ('PAID', 'CREDIT')
        """)
    BigDecimal sumQuantitySoldByProductAndPeriod(
            @Param("productId") Long productId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
        select coalesce(sum(i.total), 0)
        from SaleOrderItem i
        where i.product.id = :productId
          and i.order.orderDate between :start and :end
          and i.order.status in ('PAID', 'CREDIT')
        """)
    BigDecimal sumRevenueSoldByProductAndPeriod(
            @Param("productId") Long productId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
        select coalesce(sum(i.quantity), 0)
        from SaleOrderItem i
        join i.order o
        join o.client c
        where i.product.id = :productId
          and c.profile.id = :profileId
          and o.orderDate between :start and :end
          and o.status in ('PAID', 'CREDIT')
        """)
    BigDecimal sumQuantitySoldByProductAndProfileAndPeriod(
            @Param("productId") Long productId,
            @Param("profileId") Long profileId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
        select coalesce(sum(i.total), 0)
        from SaleOrderItem i
        join i.order o
        join o.client c
        where i.product.id = :productId
          and c.profile.id = :profileId
          and o.orderDate between :start and :end
          and o.status in ('PAID', 'CREDIT')
        """)
    BigDecimal sumRevenueSoldByProductAndProfileAndPeriod(
            @Param("productId") Long productId,
            @Param("profileId") Long profileId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
