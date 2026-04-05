package com.ecoamazonas.eco_agua.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    @Query("""
            select distinct o
            from SaleOrder o
            join fetch o.client c
            left join fetch c.profile cp
            left join fetch o.items i
            where c.id = :clientId
            order by o.orderDate desc, o.id desc
            """)
    List<SaleOrder> findDetailedOrdersByClientIdOrderByOrderDateDescIdDesc(
            @Param("clientId") Long clientId
    );

    @Query("""
            select distinct o
            from SaleOrder o
            join fetch o.client c
            left join fetch c.profile cp
            left join fetch o.items i
            where c.id = :clientId
              and o.orderDate between :startDate and :endDate
            order by o.orderDate desc, o.id desc
            """)
    List<SaleOrder> findDetailedOrdersByClientIdAndOrderDateBetweenOrderByOrderDateDescIdDesc(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select distinct o
            from SaleOrder o
            join fetch o.client c
            left join fetch c.profile cp
            left join fetch o.items i
            order by c.name asc, o.orderDate desc, o.id desc
            """)
    List<SaleOrder> findDetailedOrdersOrderByClientNameAscOrderDateDescIdDesc();

    @Query("""
            select distinct o
            from SaleOrder o
            join fetch o.client c
            left join fetch c.profile cp
            left join fetch o.items i
            where o.orderDate between :startDate and :endDate
            order by c.name asc, o.orderDate desc, o.id desc
            """)
    List<SaleOrder> findDetailedOrdersByOrderDateBetweenOrderByClientNameAscOrderDateDescIdDesc(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select distinct o
            from SaleOrder o
            join fetch o.client c
            left join fetch c.profile cp
            where o.orderDate between :startDate and :endDate
              and o.status = :status
            order by o.orderDate desc, o.id desc
            """)
    List<SaleOrder> findCreditOrdersWithClientByOrderDateBetweenAndStatusOrderByOrderDateDescIdDesc(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") OrderStatus status
    );

    @Query("""
            select distinct o
            from SaleOrder o
            join fetch o.client c
            left join fetch c.profile cp
            left join fetch o.items i
            where o.id = :id
            """)
    Optional<SaleOrder> findDetailedById(@Param("id") Long id);
}