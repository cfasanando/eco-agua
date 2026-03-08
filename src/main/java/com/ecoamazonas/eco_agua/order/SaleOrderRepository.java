package com.ecoamazonas.eco_agua.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SaleOrderRepository extends JpaRepository<SaleOrder, Long> {

    long countByOrderDate(LocalDate orderDate);

    List<SaleOrder> findByOrderDate(LocalDate orderDate);

    List<SaleOrder> findByOrderDateAndStatus(LocalDate orderDate, OrderStatus status);

    List<SaleOrder> findByOrderDateBetweenAndStatus(
            LocalDate startDate,
            LocalDate endDate,
            OrderStatus status
    );

    List<SaleOrder> findByOrderDateBetween(
            LocalDate startDate,
            LocalDate endDate
    );

    List<SaleOrder> findByStatusOrderByOrderDateAscIdAsc(OrderStatus status);

    @Query("""
        select distinct o.client.id
        from SaleOrder o
        where o.orderDate = :orderDate
          and o.status in :statuses
          and o.client is not null
        """)
    List<Long> findDistinctClientIdsWithOrderOnDate(
            @Param("orderDate") LocalDate orderDate,
            @Param("statuses") List<OrderStatus> statuses
    );

    @Query("""
        select o
        from SaleOrder o
        join fetch o.client c
        left join fetch c.profile p
        where o.status in :statuses
          and o.orderDate between :startDate and :endDate
        order by c.id asc, o.orderDate asc, o.id asc
        """)
    List<SaleOrder> findHistoricalOrdersForSuggestionBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<OrderStatus> statuses
    );
}