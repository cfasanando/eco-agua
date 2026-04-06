package com.ecoamazonas.eco_agua.delivery;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SaleOrderDeliveryEventRepository extends JpaRepository<SaleOrderDeliveryEvent, Long> {
    List<SaleOrderDeliveryEvent> findBySaleOrderIdOrderByEventDateDescIdDesc(Long saleOrderId);
}
