package com.ecoamazonas.eco_agua.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface SaleOrderPaymentRepository extends JpaRepository<SaleOrderPayment, Long> {

    List<SaleOrderPayment> findBySaleOrderIdOrderByPaymentDateAscIdAsc(Long saleOrderId);

    List<SaleOrderPayment> findBySaleOrderIdInOrderBySaleOrderIdAscPaymentDateAscIdAsc(Collection<Long> saleOrderIds);

    @Query("""
            select p
            from SaleOrderPayment p
            join fetch p.saleOrder o
            left join fetch o.client c
            where p.paymentDate between :startDate and :endDate
            order by p.paymentDate asc, p.id asc
            """)
    List<SaleOrderPayment> findCashflowPaymentsBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
