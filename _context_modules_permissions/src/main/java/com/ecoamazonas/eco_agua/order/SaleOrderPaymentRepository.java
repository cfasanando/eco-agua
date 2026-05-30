package com.ecoamazonas.eco_agua.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SaleOrderPaymentRepository extends JpaRepository<SaleOrderPayment, Long> {

    List<SaleOrderPayment> findBySaleOrderIdOrderByPaymentDateAscIdAsc(Long saleOrderId);

    List<SaleOrderPayment> findBySaleOrderIdInOrderBySaleOrderIdAscPaymentDateAscIdAsc(Collection<Long> saleOrderIds);
}