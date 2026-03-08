package com.ecoamazonas.eco_agua.order;

import org.springframework.data.jpa.repository.JpaRepository;

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
}