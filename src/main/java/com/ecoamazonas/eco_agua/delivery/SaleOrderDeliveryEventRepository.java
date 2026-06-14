package com.ecoamazonas.eco_agua.delivery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleOrderDeliveryEventRepository extends JpaRepository<SaleOrderDeliveryEvent, Long> {
    List<SaleOrderDeliveryEvent> findBySaleOrderIdOrderByEventDateDescIdDesc(Long saleOrderId);

    @Query("""
            select e
            from SaleOrderDeliveryEvent e
            left join fetch e.saleOrder o
            left join fetch o.client c
            where e.eventDate >= :startDateTime
              and e.eventDate < :endDateTime
            order by e.eventDate desc, e.id desc
            """)
    List<SaleOrderDeliveryEvent> findDashboardEventsBetween(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}
