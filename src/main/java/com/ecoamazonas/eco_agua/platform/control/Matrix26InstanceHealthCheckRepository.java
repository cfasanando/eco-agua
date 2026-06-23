package com.ecoamazonas.eco_agua.platform.control;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Matrix26InstanceHealthCheckRepository extends JpaRepository<Matrix26InstanceHealthCheck, Long> {

    Optional<Matrix26InstanceHealthCheck> findTopByInstance_IdOrderByCheckedAtDesc(Long instanceId);

    List<Matrix26InstanceHealthCheck> findTop20ByOrderByCheckedAtDesc();
}
