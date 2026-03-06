package com.ecoamazonas.eco_agua.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface SaleOrderItemRepository extends JpaRepository<SaleOrderItem, Long> {

    /**
     * Sums the quantity sold for a product in a date range.
     * Canceled orders are excluded.
     */
    @Query("""
        select coalesce(sum(i.quantity), 0)
        from SaleOrderItem i
        where i.product.id = :productId
          and i.order.orderDate between :start and :end
          and i.order.status <> 'CANCELED'
        """)
    BigDecimal sumQuantitySoldByProductAndPeriod(
            @Param("productId") Long productId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
