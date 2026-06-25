package com.ecoamazonas.eco_agua.platform.control.operations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface Matrix26RuntimeOperationRepository extends JpaRepository<Matrix26RuntimeOperation, Long> {

    List<Matrix26RuntimeOperation> findTop100ByOrderByRequestedAtDesc();

    List<Matrix26RuntimeOperation> findTop20ByInstance_IdOrderByRequestedAtDesc(Long instanceId);

    Optional<Matrix26RuntimeOperation> findTopByInstance_IdOrderByRequestedAtDesc(Long instanceId);

    boolean existsByInstance_IdAndStatusIn(
            Long instanceId,
            Collection<Matrix26RuntimeOperationStatus> statuses
    );

    List<Matrix26RuntimeOperation> findByStatusInOrderByRequestedAtAsc(
            Collection<Matrix26RuntimeOperationStatus> statuses
    );
}
