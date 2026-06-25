package com.ecoamazonas.eco_agua.platform.control.operations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Matrix26RuntimeManagedStateRepository extends JpaRepository<Matrix26RuntimeManagedState, Long> {

    Optional<Matrix26RuntimeManagedState> findByInstance_Id(Long instanceId);

    Optional<Matrix26RuntimeManagedState> findByRuntimeKey(String runtimeKey);
}
