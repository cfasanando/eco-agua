package com.ecoamazonas.eco_agua.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, Long> {

    List<DeliveryZone> findAllByOrderByNameAsc();
}
