package com.ecoamazonas.eco_agua.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryImportStopRepository extends JpaRepository<DeliveryImportStop, Long> {

    List<DeliveryImportStop> findByBatchIdOrderByRouteOrderIndexAscIdAsc(Long batchId);
}
