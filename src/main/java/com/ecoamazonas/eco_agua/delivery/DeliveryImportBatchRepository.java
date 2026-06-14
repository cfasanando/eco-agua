package com.ecoamazonas.eco_agua.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryImportBatchRepository extends JpaRepository<DeliveryImportBatch, Long> {

    List<DeliveryImportBatch> findTop20ByOrderByRouteDateDescIdDesc();
}
