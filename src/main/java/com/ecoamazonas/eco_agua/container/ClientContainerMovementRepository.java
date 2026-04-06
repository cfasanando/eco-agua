package com.ecoamazonas.eco_agua.container;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientContainerMovementRepository extends JpaRepository<ClientContainerMovement, Long> {

    List<ClientContainerMovement> findAllByOrderByMovementDateAscIdAsc();

    List<ClientContainerMovement> findByClientIdOrderByMovementDateDescIdDesc(Long clientId);

    List<ClientContainerMovement> findBySaleOrderIdOrderByMovementDateAscIdAsc(Long saleOrderId);
}
